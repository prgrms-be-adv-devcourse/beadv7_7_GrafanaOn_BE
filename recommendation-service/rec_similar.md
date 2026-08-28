# 유사 상품 조회

`product_vector` 에 쌓인 벡터로 **유사 상품 ID 목록을 응답하기까지의 현재 구현**을 정리한 문서입니다.

왜 그렇게 정했는지는 [rec_recommendation.md](./rec_recommendation.md), 벡터가 쌓이는 과정은 [rec_embedding.md](./rec_embedding.md) 에 있습니다.

| 항목        | 현재 상태                                          |
| --------- | ---------------------------------------------- |
| 엔드포인트     | `GET /api/recommendations/similar`             |
| 기준        | **회원이 아니라 상품**. `productId` 하나                  |
| 거리        | Cosine Distance (`<=>`). 작을수록 유사               |
| Threshold | `0.45` — 시드 147건으로 측정한 잠정값 ([6장](#6-threshold)) |
| 조회 방식     | Raw SQL + `JdbcTemplate`. self join, Index 없음   |
| 활성 조건     | 없음. 임베딩 파이프라인과 무관하게 항상 떠 있다                    |
| 검증        | 단위/슬라이스 11건 + 로컬 실호출 ([7장](#7-검증))              |

---

## 1. 전체 흐름

```text
GET /api/recommendations/similar?productId=101&size=10
        │
        ▼
RecommendationController
        │  size 는 선택. 그대로 넘긴다
        ▼
RecommendationService            [트랜잭션 없음]
        │
        ├─ 1. productId 검증 ─────────── null → BusinessException (400, RC-002)
        │
        ├─ 2. size 정규화 ────────────── null / 1 미만 → 기본값, 상한 초과 → 상한
        │
        └─ 3. 조회 (maxDistance, limit 을 붙여서)
                 │
                 ▼
        ProductVectorRepository (포트)
                 │
        ProductVectorJdbcAdapter         [JdbcTemplate]
                 │  self join 1회. 계산은 전부 DB 안에서 끝난다
                 ▼
              pgvector
                 │
                 ▼
        List<RecommendationItem>   (productId, distance) — 가까운 순
                 │
                 ▼
RecommendSimilarItemsResponse.listOf()
                 │  인덱스를 rank 로 바꾸고 distance 는 버린다
                 ▼
        [{productId, rank}, ...]
```

임베딩 파이프라인과 달리 **트랜잭션도 스케줄러도 없습니다.** 읽기 한 문장이 전부입니다.

---

## 2. 구성 요소

| 파일                                                        | 역할                                          |
| --------------------------------------------------------- | ------------------------------------------- |
| `presentation/RecommendationController`                    | 파라미터 바인딩, `rank` 부여 위임                      |
| `presentation/dto/response/RecommendSimilarItemsResponse`  | 응답 1건. 목록 → `rank` 매핑                       |
| `application/RecommendationService`                        | `productId` 검증, `size` 정규화, 포트 호출            |
| `domain/model/RecommendationItem`                          | 조회 결과 1건 `(productId, distance)`             |
| `domain/repository/ProductVectorRepository`                | 저장소 **포트**. 기술을 모른다                         |
| `infrastructure/persistence/ProductVectorJdbcAdapter`      | 포트의 pgvector 구현. `findSimilar` — 유사도 SQL     |
| `domain/exception/RecommendationErrorCode`                 | `RC-002 PRODUCT_ID_REQUIRED`                 |

테스트: `application/RecommendationServiceTest` (7건), `presentation/RecommendationControllerTest` (4건)

### 2.1 인프라의 벡터와 도메인의 벡터를 나눕니다

임베딩 모델을 부르는 것은 **인프라의 관심사**입니다. 추천 서비스는 그 결과를 받아서 쓸 뿐입니다. 그래서 벡터를 표현하는 타입이 경계 양쪽에 하나씩 있습니다.

| 경계   | 타입                                | 아는 것                            |
| ---- | --------------------------------- | ------------------------------- |
| 도메인  | `Embedding` (`modelName` + 값)      | 모델명과 차원. 값의 의미는 모른다             |
| 인프라  | `PGvector` / `product_vector` 행    | 드라이버 바인딩, SQL, `embedded_at`     |

둘 사이의 변환은 어댑터의 `new PGvector(embedding.values())` **한 줄**이고, 그 한 줄이 경계입니다.

`Embedding` 을 **불투명한 값**으로 둔 근거는 코드에 있습니다. `application` 은 `float[]` 를 선언하고 받아서 넘길 뿐 **원소를 한 번도 읽지 않습니다.** 재임베딩 판단은 `story` 문자열 비교이고, 거리 계산은 pgvector 안에서 끝납니다. 그래서 값을 해석하는 메서드를 두지 않았습니다. 코사인 유사도를 자바로 계산하고 싶어지면 그건 이 타입이 아니라 저장소가 할 일입니다.

`modelName` 을 벡터와 한 몸으로 묶은 이유는, 둘이 떨어지는 순간 **어느 공간의 좌표인지 모르는 숫자 배열**이 되기 때문입니다. 좌표는 그 좌표계 안에서만 의미가 있습니다. 이 값은 `product_vector.model_name` 에 기록으로 남고 조회 조건으로는 쓰지 않습니다([4.2](#42-model_name-은-조건에-걸지-않는다)).

행을 그대로 옮겨 담는 별도의 `Row` 클래스는 두지 않았습니다. `JdbcTemplate` 을 쓰는 한 인프라 쪽 표현은 이미 `PGvector` 와 SQL 이고, 한 줄 변환만 하는 클래스를 사이에 끼우면 껍데기만 늘어납니다. JPA 나 다른 저장소가 들어오면 그때 생깁니다.

#### 행을 통째로 감싸는 타입은 두지 않습니다

`product_vector` 한 행에 대응하는 `ProductVector` 타입을 만들었다가 지웠습니다. 저장소 포트는 값을 그대로 받습니다.

```java
void save(Long productId, String story, Embedding embedding);
```

`product_id` 가 PK 이고 저장이 upsert 라 **모델링만 보면 식별자를 가진 Entity 가 맞습니다.** 재임베딩된 상품은 남남이 아니라 같은 것의 다음 상태이므로, 타입을 둔다면 동일성은 `productId` 로 판단해야 합니다.

그런데 그 동일성을 **쓰는 곳이 없었습니다.** 만들자마자 저장하고 버려서 다시 읽지도, 바꾸지도, 비교하지도 않고, `Set`/`Map` 에 담지도 않습니다. 실제로 `hashCode()` 가 통째로 빠졌는데 런타임에는 아무 일도 일어나지 않았고 그것을 검증하려고 쓴 테스트 하나만 깨졌습니다. **아무도 보지 않는 코드는 틀려도 드러나지 않습니다.**

게다가 그 동일성이 테스트를 나쁘게 만들었습니다. `equals` 가 `productId` 만 보니 `commit()` 에 넘어간 `story` 가 틀려도 통과해서, `ArgumentCaptor` 로 꺼내 필드를 하나씩 봐야 했습니다. 타입을 없애니 이렇게 됩니다.

```java
then(embeddingCommitService).should().save(event, PRODUCT_ID, "이야기", embedding);
```

**다시 만들 시점**은 둘 중 하나입니다 — 이 행을 통째로 읽어오는 경로가 생기거나(그때 `reembed` 도 함께), 메타데이터 컬럼이 붙어([rec_recommendation.md 8.3](./rec_recommendation.md#83-부가정보만-변경되는-경우)) 인자가 늘어날 때입니다.

`recommendation_inbox` 는 아예 도메인에 두지 않았습니다. 전송 방식을 바꾸면 사라질 개념이라 인프라로 봅니다. ([rec_embedding.md 2.1](./rec_embedding.md#21-inbox-는-인프라입니다))

#### 그래서 남은 타입은 전부 값입니다

| 타입                   | 형태       | 왜                                      |
| -------------------- | -------- | -------------------------------------- |
| `Embedding`          | `record` | 식별자가 없다. 내용이 곧 정체성이다                   |
| `RecommendationItem` | `record` | 질의 결과다. `distance` 는 기준 상품에 대해서만 의미가 있다 |

이 서비스는 도메인 객체를 읽어와 상태를 바꾸는 곳이 없습니다. 행을 쓰고 집계를 읽을 뿐입니다. Entity 가 없는 것이 그 사실을 정직하게 드러냅니다.

### 2.2 `RecommendationItem` 은 Entity 가 아닙니다

`domain/model` 아래 있지만 **JPA Entity 가 아니라 값(record)** 입니다. `recommendation` 스키마에 대응하는 테이블이 없기 때문입니다. 벡터는 `product_vector` 에만 있고, 그 테이블은 Hibernate 에 `vector` 타입이 없어 `JdbcTemplate` 이 다룹니다. ([rec_recommendation.md 6.3](./rec_recommendation.md#63-schema))

Entity 로 두면 `ddl-auto=update` 가 쓰지도 않는 빈 테이블을 만들고, 그 테이블에 없는 컬럼을 조건으로 걸면 조회가 런타임에 터집니다.

### 2.3 왜 조회에는 `pipeline.enabled` 가 걸려 있지 않은가

임베딩 파이프라인 빈들은 전부 `recommendation.pipeline.enabled` 에 걸려 있습니다. `EmbeddingModel` 이 없으면 만들어질 수 없기 때문입니다. ([rec_embedding.md 5.1](./rec_embedding.md#51-pipelineenabled))

조회는 다릅니다. **`product_vector` 를 읽기만 하므로 `EmbeddingModel` 이 필요 없습니다.** 기준 상품을 다시 임베딩하지 않고 이미 저장된 벡터를 그대로 쓰기 때문입니다. pgvector 를 고른 핵심 이유가 이것이었습니다. ([rec_recommendation.md 3.3](./rec_recommendation.md#33-pgvector))

`RecommendationService` 가 `ProductEmbedder` 포트를 주입받지 않는 이유가 이것입니다. 그 포트를 잡으면 `EmbeddingModel` 빈이 없는 운영에서 **조회까지 같이 죽습니다.**

`RECOMMENDATION_PIPELINE_ENABLED=false` 로 기동해 확인했습니다. `SpringAiProductEmbedder` 와 `ProductEventHandler` 가 만들어지지 않은 상태에서 조회는 정상 응답합니다. 벡터가 아직 없으면 빈 배열이 나갈 뿐입니다.

---

## 3. API

### 3.1 요청

```http
GET /api/recommendations/similar?productId=101&size=10
```

| 파라미터        | 필수 | 기본값 | 설명                           |
| ----------- | -- | --- | ---------------------------- |
| `productId` | O  | —   | 기준 상품. 이 상품의 벡터와 나머지를 비교한다   |
| `size`      | X  | 10  | 받을 개수. 1 미만이면 기본값, 50 초과면 50 |

**회원 식별자를 받지 않습니다.** 추천 근거가 Story 벡터 하나뿐이라 같은 상품을 보는 사람에게는 누구에게나 같은 결과가 나옵니다. 회원의 관심 상품 여러 개를 묶어 하나의 추천을 만드는 것은 입력도 결과도 다른 일이라 별도 엔드포인트로 둡니다. ([8.3](#83-관심-상품-기반-추천))

`size` 는 **거절하지 않고 범위 안으로 접습니다.** 추천 개수는 요청자가 "틀릴" 수 있는 값이 아니라 취향에 가깝습니다. 상한을 두는 이유는 Index 가 없어 `LIMIT` 이 곧 조회 비용이기 때문입니다.

### 3.2 응답

```json
{
  "code": "success",
  "message": "API 요청이 정상적으로 수행되었습니다.",
  "data": [
    { "productId": 103, "rank": 1 },
    { "productId": 102, "rank": 2 },
    { "productId": 104, "rank": 3 }
  ]
}
```

**`distance` 를 그대로 내보내지 않습니다.** 값의 의미가 임베딩 모델과 Threshold 에 묶여 있어 클라이언트가 해석할 수 있는 수치가 아닙니다. 모델을 `bge-m3` 에서 `text-embedding-3-small` 로 바꾸면 같은 상품 쌍의 거리도 달라집니다. 대신 **순서만** `rank` 로 줍니다.

상품의 이름·가격·이미지는 담지 않습니다. 이 서비스에는 그 데이터가 없습니다. `productId` 로 `commerce-service` 에서 조회하는 것은 호출자의 몫입니다.

### 3.3 응답 상태

| 상황              | 상태  | 본문           |
| --------------- | --- | ------------ |
| 정상              | 200 | 가까운 순 목록     |
| 기준 상품에 벡터가 없음   | 200 | `"data": []` |
| Threshold 안에 없음 | 200 | `"data": []` |
| `productId` 누락  | 400 | `RC-002`     |

**벡터가 없어도 404 가 아닙니다.** "그런 상품이 없다" 가 아니라 "아직 임베딩되지 않았다" 는 뜻이기 때문입니다. 파이프라인이 비동기라 상품 등록 직후에는 정상적으로 비어 있습니다. 이 서비스는 상품의 존재 여부를 알지 못하므로 판단할 수도 없습니다.

> ⚠️ `productId` 를 `@RequestParam(required = false)` 로 받고 **서비스가 거절**합니다.
> `required = true` 로 두면 `MissingServletRequestParameterException` 이 발생하는데,
> `CommonExceptionHandler` 에 이 예외의 핸들러가 없어 catch-all 로 떨어져 **500** 이 나갑니다.
> 공통 모듈에 핸들러가 추가되면 `required = true` 로 되돌리고 서비스의 검증을 지울 수 있습니다.

---

## 4. 조회 쿼리

```sql
SELECT s.product_id                AS product_id,
       s.embedding <=> b.embedding AS distance
  FROM recommendation.product_vector s,      -- s = 후보
       recommendation.product_vector b       -- b = 기준
 WHERE b.product_id  = ?
   AND b.model_name  = ?
   AND s.model_name  = b.model_name
   AND s.product_id <> b.product_id
   AND (s.embedding <=> b.embedding) < ?
 ORDER BY distance
 LIMIT ?
```

### 4.1 기준 벡터를 애플리케이션으로 꺼내지 않는다

같은 테이블을 self join 해서 **기준 행(`b`)과 후보 행(`s`)이 한 쿼리 안에서 만납니다.** 벡터를 조회해서 다시 바인딩하는 왕복이 없습니다.

그렇게 하지 않으면 1024차원(운영 1536) `float` 배열을 DB → 애플리케이션 → DB 로 실어 나르게 되고, 쿼리도 두 번이 됩니다.

Spring AI 의 `VectorStore.similaritySearch` 는 **질의 텍스트를 매번 임베딩**해서 벡터를 만듭니다. 기준이 이미 저장된 상품이면 그 호출이 통째로 낭비입니다. Raw SQL 을 택한 이유 중 하나가 이것입니다. ([rec_recommendation.md 6.2](./rec_recommendation.md#62-raw-sql))

### 4.2 `model_name` 은 조건에 걸지 않는다

모델이 다르면 **벡터 공간 자체가 다릅니다.** 다른 모델로 만든 두 벡터 사이의 거리는 숫자가 나오긴 해도 의미가 없습니다. 그런데도 조건에서 뺐습니다. **한 테이블에 두 모델의 벡터가 섞이지 않는다**는 전제이기 때문입니다.

차원이 다른 모델로 바꾸면 섞일 수가 없습니다. 컬럼 타입부터 맞지 않아 적재 자체가 거부됩니다.

```text
INSERT 1536차원 → VECTOR(1024) 컬럼
  ERROR: expected 1024 dimensions, not 1536
```

즉 예정된 전환(로컬 `bge-m3` 1024 → 운영 `text-embedding-3-small` 1536)은 **스키마 재구축이 강제**되므로, 재구축 이후 테이블 안의 `model_name` 은 항상 한 종류입니다. 조건을 걸어도 늘 참이라 하는 일이 없습니다.

남는 경우는 **차원이 같은 모델로 갈아탈 때** 하나입니다(예: `text-embedding-3-small` → `text-embedding-3-large` 를 `dimensions=1536` 으로). 이때는 스키마가 안 바뀌어 옛 벡터가 남을 수 있고, 거리는 조용히 계산됩니다.

```text
101 ↔ 다른 모델로 만든 1024차원 벡터  →  distance 1.0241   (에러 없음)
```

그래서 이 한 경우를 코드가 아니라 **운영 절차로 막습니다** — 모델을 바꾸면 테이블을 재구축하고 백필합니다([8.1](#81-모델-교체-절차)). 조건과 그 값을 나르던 `EmbeddingModelName` / `EmbeddingModelConfig` 는 그래서 없앴습니다.

`model_name` **컬럼은 남깁니다.** 조회에는 안 쓰지만 "이 벡터를 무엇이 만들었는가" 를 남기는 기록이고, 재구축을 깜빡했는지 확인할 때 이 컬럼을 봅니다.

```sql
-- 섞여 있으면 재구축이 안 된 것이다
SELECT model_name, count(*) FROM recommendation.product_vector GROUP BY model_name;
```

### 4.3 테이블명을 스키마로 수식한다

`JdbcTemplate` 은 Hibernate 의 `default_schema` 를 따르지 않고 커넥션의 `search_path` 를 그대로 탑니다. 기본값이 `"$user", public` 이라 `recommendation` 이 들어 있지 않습니다. 수식하지 않으면 이렇게 죽습니다.

```text
ERROR: relation "product_vector" does not exist
```

`recommendation.vector.schema` 값으로 `ProductVectorJdbcAdapter` 생성자에서 한 번 조립해 둡니다.

### 4.4 바인딩은 전부 위치(`?`)

`JdbcTemplate` 은 `:name` 문법을 모릅니다. 그건 `NamedParameterJdbcTemplate` 의 것입니다. 섞어 쓰면 `:` 가 SQL 에 그대로 나가 문법 오류로 죽습니다.

### 4.5 실행 계획

벡터 147건 기준입니다. Index 를 만들지 않았으므로 Seq Scan 입니다. ([rec_recommendation.md 9.1](./rec_recommendation.md#91-vector-index))

```text
Limit (actual time=2.250..2.265 rows=10)
  -> Sort (Sort Method: quicksort  Memory: 25kB)
       -> Nested Loop (rows=16)
            Join Filter: (s.product_id <> b.product_id)
                     AND ((s.embedding <=> b.embedding) < '0.45')
            Rows Removed by Join Filter: 131
            -> Seq Scan on product_vector b   (rows=1,   Rows Removed by Filter: 146)
            -> Seq Scan on product_vector s   (rows=147)

Planning Time: 3.370 ms
Execution Time: 3.787 ms
```

기준 행을 찾는 `Seq Scan on b` 가 146건을 버리고 1건을 남깁니다. `product_id` 가 PK 인데도 Seq Scan 인 것은 **전체가 147행뿐이라 Index 를 타는 것보다 통째로 읽는 게 싸기 때문**입니다. 행이 늘면 이쪽은 자동으로 Index Scan 으로 바뀝니다.

문제는 후보 쪽입니다. `Seq Scan on s` 는 **147행 전부에 대해 거리를 계산**합니다. 이건 행 수에 비례해 늘어나고, Vector Index 없이는 줄지 않습니다. 지금은 3.8ms 지만 상품이 10만 건이면 같은 구조로 선형 증가합니다.

**Index 도입 판단은 이 수치를 다시 떠서 합니다.** 지금 넣으면 147행짜리 테이블에 HNSW 를 얹어 Build 비용과 Recall 손실만 얻습니다.

---

## 5. 설정

```yaml
recommendation:
  similar:
    max-distance: ${RECOMMENDATION_MAX_DISTANCE:0.45}
    default-size: ${RECOMMENDATION_DEFAULT_SIZE:10}
    max-size:     ${RECOMMENDATION_MAX_SIZE:50}
```

프로필로 나누지 않고 `application.yaml` 한 곳에 둡니다. 임베딩 모델과 무관한 값이기 때문입니다.

조회 경로는 `recommendation.embedding.model-name` 을 읽지 않습니다. 그 값을 읽는 곳은 `SpringAiProductEmbedder` 하나뿐이고, 벡터를 만든 쪽이 `product_vector.model_name` 에 기록으로 찍기 위해서입니다([4.2](#42-model_name-은-조건에-걸지-않는다)). 값은 프로필마다 다릅니다 (로컬 `bge-m3` / 운영 `text-embedding-3-small`).

> ⚠️ 테스트 클래스패스의 `application.yaml` 은 main 의 것을 **대체**합니다(병합이 아닙니다).
> `RecommendationService` 가 `@Value` 로 읽으므로 `src/test/resources/application.yaml` 에도
> 같은 키가 있어야 컨텍스트가 뜹니다.

---

## 6. Threshold

[rec_recommendation.md 9.2](./rec_recommendation.md#92-similarity-threshold) 가 "실제 데이터를 수집한 뒤 결정한다" 고 남겨 둔 값입니다. 시드 Story 147건(`bge-m3`, 1024차원)으로 측정했습니다.

### 6.1 거리 분포

시드는 카테고리별로 ID 가 묶여 있습니다(101~112 운동화, 201~210 가방, …). 같은 묶음에 있으면 "유사해야 하는 쌍" 으로 보고 분포를 떴습니다.

| 쌍        | 개수    | 최소    | p05   | 중앙값   | 최대    |
| -------- | ----- | ----- | ----- | ----- | ----- |
| 같은 카테고리  | 458   | 0.218 | 0.365 | 0.479 | 0.650 |
| 다른 카테고리  | 4,492 | 0.256 | 0.446 | 0.546 | 0.732 |

**깨끗하게 갈리지 않습니다.** 같은 카테고리 중앙값(0.479)이 다른 카테고리 최소값(0.256)보다 큽니다. 겹치는 구간이 분포의 대부분입니다.

당연한 결과이기도 합니다. 임베딩한 것은 카테고리가 아니라 **Story** 입니다. 카테고리가 달라도 맥락이 같은 상품이 시드에 일부러 섞여 있습니다 — 104/109 러닝화, 304 러닝 워치, 608 러닝 바람막이, 1009 마라톤 메달. 이런 쌍이 가까이 나오는 것은 오히려 의도한 동작입니다.

### 6.2 Threshold 별 결과

| threshold | 같은 카테고리 통과 | 다른 카테고리 통과 | 같은 카테고리 비율 | 재현율   |
| --------- | ---------- | ---------- | ---------- | ----- |
| 0.30      | 6          | 1          | 85.7%      | 1.3%  |
| 0.35      | 19         | 11         | 63.3%      | 4.1%  |
| 0.40      | 56         | 46         | **54.9%**  | 12.2% |
| 0.45      | 150        | 270        | 35.7%      | 32.8% |
| 0.50      | 305        | 979        | 23.8%      | 66.6% |

전체 쌍에서 같은 카테고리가 차지하는 비율은 9.3%(458/4,950)입니다. 어느 지점이든 그보다는 높으므로 **거리가 신호를 담고 있다는 것 자체는 확인됩니다.**

### 6.3 왜 0.45 인가

정확도만 보면 0.40 이 무릎입니다. 그런데 이 값은 순위를 정하지 않습니다. 순위는 `ORDER BY distance LIMIT k` 가 정하고, **Threshold 가 하는 일은 "이 아래로는 아예 추천하지 않는다" 는 바닥선을 긋는 것뿐**입니다.

바닥선을 높게 잡으면 상품이 적을 때 추천이 통째로 비어버립니다. 0.40 은 같은 카테고리 쌍의 12%만 통과시킵니다. 그래서 **일단 0.45 로 두고 상품이 쌓이면 다시 측정합니다.** 값은 `RECOMMENDATION_MAX_DISTANCE` 로 코드 수정 없이 바꿀 수 있습니다.

이 판단은 시드 데이터 기준입니다. 합성 문장 100건은 전부 50자 안팎이라 길이 편차가 없습니다. **운영 Story(42~566자)로 다시 떠야 확정할 수 있습니다.**

---

## 7. 검증

### 7.1 준비

```bash
# rec_embedding.md 6.1 로 벡터를 채운 뒤
./gradlew :recommendation-service:bootRun
```

### 7.2 실호출

벡터 147건(`bge-m3`) 기준입니다.

```bash
curl "localhost:8083/api/recommendations/similar?productId=101&size=5"
```

```json
// data 필드만 옮긴 것입니다. 전체 응답 형태는 3.2 참고
[{"productId":103,"rank":1},{"productId":102,"rank":2},{"productId":104,"rank":3},
 {"productId":2000034,"rank":4},{"productId":201,"rank":5}]
```

101 은 조던1 시카고입니다. **1~3위가 전부 운동화(102~104)** 로 나왔습니다. 4위부터 카테고리를 가로지릅니다.

| 케이스                         | 기대             | 결과                           |
| --------------------------- | -------------- | ---------------------------- |
| `productId=101&size=5`      | 가까운 순 5건       | 103 / 102 / 104 / … rank 1~5 |
| `size` 생략                   | 기본 10건         | 10건                          |
| `size=999`                  | 상한 50 으로 절삭    | 200                          |
| `productId=999999` (벡터 없음)  | 200 + 빈 배열     | `"data": []`                 |
| `productId` 누락              | 400 + `RC-002` | `기준 상품 ID 가 필요합니다.`          |

거리까지 확인하려면 SQL 로 직접 봅니다.

```sql
SELECT s.product_id,
       round((s.embedding <=> b.embedding)::numeric, 4) AS distance
  FROM recommendation.product_vector s, recommendation.product_vector b
 WHERE b.product_id = 101
   AND s.product_id <> b.product_id
   AND (s.embedding <=> b.embedding) < 0.45
 ORDER BY distance LIMIT 10;

-- 103 0.3069 / 102 0.3513 / 104 0.3687 / 2000034 0.3705 / 201 0.3897 / ...
```

### 7.3 자동화된 검증

```text
RecommendationServiceTest (7)
  · Threshold 와 limit 을 그대로 조회 조건으로 넘긴다
  · size 가 없거나 1 미만이면 기본값을 쓴다        [null, 0, -1]
  · size 가 상한을 넘으면 상한으로 자른다
  · 조회 결과가 없으면 빈 리스트를 반환한다
  · 저장소가 돌려준 가까운 순서를 그대로 유지한다

RecommendationControllerTest (4)
  · 가까운 순으로 rank 를 1부터 매겨 응답한다
  · size 를 생략하면 null 로 넘겨 서비스의 기본값을 쓰게 한다
  · 유사 상품이 없으면 200 과 빈 배열을 반환한다
  · productId 가 없으면 400 과 에러 코드를 반환한다
```

SQL 자체는 이 테스트들이 검증하지 않습니다. 포트인 `ProductVectorRepository` 를 mock 으로 두기 때문입니다. 쿼리는 [7.2](#72-실호출) 의 실호출과 [4.5](#45-실행-계획) 의 실행 계획으로 확인합니다.

---

## 8. 운영 / 남은 것

### 8.1 모델 교체 절차

조회 SQL 이 `model_name` 을 보지 않으므로([4.2](#42-model_name-은-조건에-걸지-않는다)), **모델을 바꿀 때 옛 벡터를 남겨두면 안 됩니다.** 차원이 바뀌면 적재가 거부되어 저절로 드러나지만, 차원이 같으면 조용히 섞입니다.

```text
1. recommendation.embedding.model-name 변경
2. recommendation.vector.dimensions 변경 (차원이 다를 때)
3. product_vector 재구축   ← 빠뜨리면 옛 벡터와 새 벡터가 섞인다
4. 전 상품 백필            ← 빠뜨리면 추천이 빈 배열로 나간다
5. 확인: SELECT model_name, count(*) FROM product_vector GROUP BY model_name;  → 1행이어야 한다
```

### 8.2 백필 경로가 없습니다

**4번을 할 수단이 아직 없습니다.** 재임베딩은 그 상품에 새 이벤트가 올 때만 일어나는데, 이벤트는 저절로 오지 않습니다. 상품을 아무도 수정하지 않으면 벡터가 영영 만들어지지 않고, 조회는 빈 배열을 돌려줍니다.

지금은 `sql/seed-product-inbox.sql` 로 손으로 밀어 넣고 있습니다. 운영에서는 둘 중 하나가 필요합니다.

* `commerce-service` 가 전 상품 이벤트를 재발행한다 (Outbox 에 다시 쌓는다)
* `recommendation-service` 가 벡터 없는 상품을 찾아 채운다 (이 서비스에는 상품 목록이 없어 조회 API 가 필요하다)

모델 교체뿐 아니라 **파이프라인을 처음 켤 때도** 같은 문제입니다. 운영은 아직 `pipeline.enabled=false` 라 벡터가 하나도 없습니다.

### 8.3 관심 상품 기반 추천

지금은 **상품 1개** 가 기준입니다. [rec_recommendation.md 2.2](./rec_recommendation.md#22-구현-방법) 가 말하는 "유저의 관심 상품" 기반 추천은 입력이 다릅니다.

```text
현재   상품 1개      → 유사 상품        (상품 상세 페이지용)
추후   관심 상품 N개  → 유사 상품        (홈 피드용)
```

N개를 하나의 기준 벡터로 만드는 방법(평균 / 최근 가중 / 각각 조회 후 병합)을 정해야 하고, 관심 상품 목록을 어디서 받을지도 정해야 합니다. 이 서비스에는 스크랩 데이터가 없습니다. 응답 형태가 지금과 같아도 **별도 엔드포인트**입니다.

### 8.4 판매 상태 필터

`product_vector` 에는 `product_id / story / model_name / embedding / embedded_at` 뿐입니다. `SOLD_OUT` 인 상품도 그대로 추천에 섞입니다.

Story 가 바뀌지 않는 부가정보 변경으로 벡터를 다시 만들 필요는 없으므로([rec_recommendation.md 8.3](./rec_recommendation.md#83-부가정보만-변경되는-경우)), 이 요구가 생기면 **메타데이터 컬럼과 그 갱신 경로**를 함께 추가해야 합니다. 호출자가 받은 ID 를 `commerce-service` 에서 걸러내는 방법도 있지만, 그러면 `size=10` 을 요청해도 실제로 보여줄 수 있는 건 그보다 적어집니다.

### 8.5 Vector Index

[4.5](#45-실행-계획) 참고. 상품이 쌓인 뒤 `EXPLAIN ANALYZE` 를 다시 떠서 판단합니다.

### 8.6 Threshold 확정

[6.3](#63-왜-045-인가) 참고. 운영 Story 로 분포를 다시 측정해야 합니다.
