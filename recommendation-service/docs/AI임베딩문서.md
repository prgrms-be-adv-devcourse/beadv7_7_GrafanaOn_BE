# 임베딩 파이프라인

| 항목     | 현재 상태                                                  |
|--------| ------------------------------------------------------ |
| 처리 방식  | `@Scheduled` 폴링 (`fixedDelay` 5초, 100건씩)           |
| 임베딩    | Ollama `bge-m3` (1024차원). 상품 1개당 벡터 1개                  |
| 저장     | `product_vector` (직접 정의한 테이블, `JdbcTemplate`)           |

---

## 1. 전체 흐름

```text
recommendation_inbox (PENDING + 시도가 남은 FAILED)
        │
        │ SpringEmbeddingScheduler — 5초마다 폴링
        ▼
ProductEventStore.findProcessable(N)
        │  occurred_at 오름차순 N건
        ▼
ProductEventHandler.process() 
        │
        ├─ 1. payload 파싱 ──────────── 실패 → FAILED (재시도 대상)
        │
        ├─ 2. 순서 역전 검사 ─────────── 예전 이벤트 → PROCESSED (임베딩 안 함)
        │
        ├─ 3. 재임베딩 필요 여부 ──────── 저장된 story 와 동일 → PROCESSED (임베딩 안 함)
        │
        ├─ 4. ProductEmbedder.embed(story)  ← 임베딩 처리. 트랜잭션 밖이라 커넥션을 잡지 않는다
        │                                        실패 → FAILED (재시도 대상)
        │
        └─ 5. EmbeddingService.save()   [트랜잭션]
                 ├─ product_vector   (INSERT ... ON CONFLICT DO UPDATE)
                 └─ Inbox PROCESSED
                                                 실패 → FAILED (재시도 대상)
```

FAILED 로 끝나지 않습니다. **대기열이 신규와 실패분을 함께 봅니다.** 실패한 이벤트도
`retry_count` 가 한도에 닿기 전까지는 다음 폴링에 같이 집혀 위 흐름을 그대로 다시 탑니다([3.7](#37-실패와-재시도)).
---

## 2. 구성 요소
### 2.1 domain
추천 서비스의 도메인 개념은 **Story, 임베딩(벡터), 유사도** 입니다. 어떤 이벤트인지(inbox)는 도메인의 관심사가 아니므로 임베딩 결과를 담는 Embedding 객체만 domain에 두었습니다.

| 파일                                                        | 역할                                            |
| --------------------------------------------------------- | --------------------------------------------- |
| `domain/model/Embedding`                                   | 임베딩 결과         |
| `domain/repository/ProductVectorRepository`                | 벡터 저장소 **포트**                                  |
### 2.2 application
임베딩 계산은 도메인 관심사가 아니라 infra관심사이므로 임베딩 수단 또한 port에 있습니다.

| 파일                                        | 역할                               |
|-------------------------------------------|----------------------------------|
| `application/handler/ProductEventHandler` | 이벤트 1건의 임베딩 처리 흐름                |
| `application/EmbeddingService`            | **벡터 저장, Inbox 상태변경 트랜잭션 처리**    |
| `application/dto/ProductPayload`          | `{ productId, story }`json 파싱 결과 |
| `application/dto/ProductEvent`            | 수신·처리 공용 이벤트 DTO                 |
| `application/port/ProductEventStore`      | 이벤트 저장소 **포트**                   |
| `application/port/ProductEmbedder`        | 임베딩 처리 **포트**                    |
### 2.3 infrastructure
인프라 관심사인 이벤트 수신 및 ai 임베딩 처리 

| 파일                                                        | 역할                                       |
| --------------------------------------------------------- |------------------------------------------|
| `infrastructure/scheduler/SpringEmbeddingScheduler`        | 스케줄러 폴링 진입점              |
| `infrastructure/inbox/RecommendationInbox`                 | Inbox  JPA 엔티티. 컬럼 매핑과 상태 전이만 가짐         |
| `infrastructure/inbox/RecommendationInboxAdapter`          | `ProductEventStore` 구현. 엔티티 ↔ `ProductEvent` 매핑 |
| `infrastructure/ai/SpringAiProductEmbedder`                  | Spring AI 구현|
| `infrastructure/persistence/ProductVectorSchemaInitializer`| 기동 시 `product_vector` 테이블 생성             |
| `infrastructure/persistence/ProductVectorJdbcAdapter`      | `product_vector` upsert / 조회 (`JdbcTemplate`) |



### 2.4 Inbox 상태값

`InboxStatus` 는 세 값입니다.

```text
  PENDING ──┬──▶ PROCESSED   임베딩 완료 / 소비만 하고 종료 (순서 역전·story 동일)
            │
            └──▶ FAILED ──┐  처리 실패
                   ▲       │
                   └───────┘  재시도. retry_count 를 올리며 PENDING 을 거치지 않는다
                             한도에 닿으면 조회에서 빠져 더 집히지 않는다
```

**FAILED 는 상태가 하나지만 두 가지를 뜻합니다** — `retry_count` 가 한도 미만이면
재시도 대기 중이고, 한도에 닿았으면 사실상 포기한 것입니다. 상태만으로는 구분되지 않아
지금은 `retry_count` 를 함께 봐야 합니다.

## 3. 단계별 동작

### 3.1 이벤트 조회

```java
findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc([PENDING, FAILED], maxRetryCount, limit)
```

**PENDING 과 FAILED 를 한 번에 꺼냅니다.** 재시도 전용 조회를 따로 두지 않았습니다([3.7](#37-실패와-재시도)).

`occurred_at ASC` 로 꺼내므로 **오래된 이벤트가 먼저** 처리됩니다. `idx_recommendation_inbox_status_occurred_at` 을 탑니다.

트랜잭션을 열지 않습니다. 조회는 그 자체로 끝나고, 쓰기는 `EmbeddingService` 가 건별로 짧은 트랜잭션을 엽니다.

`fixedDelay` 라 이전 실행이 끝난 뒤에야 다음 실행이 시작됩니다. **한 인스턴스 안에서는** 배치가 겹치지 않습니다.

1건이 예외로 터져도 나머지는 계속 처리합니다.

### 3.2 순서 역전 검사

```java
LocalDateTime lastProcessed = eventStore.findLatestProcessedOccurredAt(event.aggregateId()).orElse(null);
if (event.isStaleAgainst(lastProcessed)) → PROCESSED 로 소비만 하고 종료
```

같은 상품에서 **이미 처리한 이벤트보다 먼저 발생한** 이벤트라면 벡터를 건드리지 않습니다. 늦게 도착한 과거 Story 가 최신 Story 를 덮어쓰는 것을 막습니다.

### 3.4 재임베딩 필요 여부

```java
embeddingService.isAlreadyEmbedded(productId, story)

// SELECT story FROM product_vector WHERE product_id = ? AND model_name = ?
//  → 이 값이 새 story 와 같으면 건너뛴다
```

`product_vector.story` 에는 임베딩에 넣은 텍스트가 그대로 들어 있습니다. 즉 **이 컬럼이 곧 "이 벡터를 만든 원문"** 이므로, 별도의 이력 테이블 없이 여기서 바로 판단합니다.

모델명을 함께 비교하는 이유는 모델이 바뀌면 벡터 공간이 달라져 Story 가 같아도 다시 임베딩해야 하기 때문입니다.

### 3.5 임베딩과 적재

```java
Embedding embedding = storyEmbedder.embed(story);                            // 트랜잭션 밖. 수백 ms ~ 수 초
commitService.save(event.id(), productId, story, embedding);                 // 트랜잭션 안. 벡터 + 상태
```
모델 호출을 트랜잭션 밖에 두어야 그 시간 동안 DB 커넥션을 붙잡지 않습니다.

* `EmbeddingModel` 을 직접 호출합니다. 임베딩 시간과 저장 시간을 따로 측정할 수 있습니다.
* `product_id` 가 PK 이므로 `INSERT ... ON CONFLICT (product_id) DO UPDATE` 로 갱신됩니다.
* 벡터는 `com.pgvector:pgvector` 의 `PGvector` 로 바인딩합니다. `PGobject` 구현체라 드라이버가 그대로 처리하므로 문자열 조립이나 `::vector` 캐스팅이 없습니다.

**청킹하지 않습니다.** 상품 1개 = 벡터 1개입니다. Spring AI 의 `TextSplitter` 는 새 `Document` 를 만들면서 `id()` 를 넘기지 않아 `productId` 가 사라집니다. 청킹을 도입하려면 "상품 1개 = 벡터 1개" 전제부터 다시 잡아야 합니다.

### 3.6 상태 갱신

inbox 이벤트를 `PROCESSED` 로 바꿉니다. 

벡터 적재와 **같은 트랜잭션**에서 일어납니다. 호출자는 `markProcessed(id)` 로 식별자만 넘기고, 어댑터가 그 트랜잭션 안에서 엔티티를 로드해 상태를 바꿉니다. 변경 감지로 커밋 시점에 UPDATE 가 나갑니다.

### 3.7 실패와 재시도

**재시도를 위한 장치가 따로 없습니다.** 대기열 조회가 처음부터 `FAILED` 를 포함하므로, 실패한 이벤트는 다음 폴링에 신규와 섞여 다시 집힙니다.

```java
where status in (PENDING, FAILED)
  and retry_count < :maxRetryCount
order by occurred_at asc
```

`PENDING` 은 `retry_count` 가 항상 0 이라 한도 조건에 걸리지 않습니다. 실패할 때만 올라가고, 실패하면 상태가 `FAILED` 로 바뀌기 때문입니다. 그래서 두 상태에 **같은 조건 하나**를 걸 수 있습니다.

집힌 뒤에는 신규와 **완전히 같은 `process()`** 를 탑니다. 재시도 전용 경로를 두면 순서 역전 검사나 재임베딩 판단이 한쪽에만 반영되는 일이 생깁니다. `process()` 는 upsert 기반이라 몇 번을 다시 돌려도 결과가 같습니다.

한도를 소진한 행은 **상태를 바꾸지 않고 조회에서 빠지는 것으로** 멈춥니다. 엔티티는 횟수와 사유만 기록하고, 어디까지 집을지는 어댑터가 판단합니다 — 한도는 **인박스의 전송 정책**이라 `application` 이 알 이유가 없습니다([2.2](#22-application)의 원칙). 포트는 `findProcessable(int limit)` 하나뿐입니다.

```java
public void markAsFailed(final String reason) {
    this.retryCount++;
    this.lastError = reason;
    this.status = InboxStatus.FAILED;
}
```

**대신 감수한 것 셋.**

* **백오프가 없습니다.** 실패한 이벤트는 다음 폴링(5초)에 곧바로 다시 시도됩니다. 모델 서버가 죽어 connection refused 로 즉시 실패하면 재시도 3번이 15초 안에 소진되므로, **1분짜리 장애는 통과하지 못하고 그때 밀려 있던 이벤트가 전부 한도에 닿습니다.** 지금 잡는 것은 단발성 실패뿐입니다.
* 실패를 한 갈래로만 봅니다. payload 파싱 실패처럼 **다시 해도 결과가 같은 것**까지 한도를 소진할 때까지 재시도합니다. 영구 실패를 위한 종착 상태(`ABANDONED` 등)를 두면 갈라낼 수 있습니다.
* 한도를 소진한 것을 사람이 알아채는 경로가 없습니다. `status` 만으로는 재시도 대기와 구분되지 않아 `retry_count >= 한도` 로 세어봐야 하고, 쌓였을 때 알려주는 것이 없습니다.

세 가지 모두 **컬럼이나 상태를 늘려야 해결되는 것**이라, 파이프라인을 운영에서 돌려보고 실제로 무엇이 실패하는지 본 뒤에 넣습니다.

---

## 4. 트랜잭션 경계와 동시성

### 4.1 경계를 어디에 두는가

한 이벤트를 처리하는 동안 트랜잭션은 **끝에 한 번만** 열립니다. 그 앞에 임베딩이 있습니다.

```text
   eventStore.findProcessable(100)                  트랜잭션 없음
   조회는 그 자체로 끝난다. 선점하지 않으므로 상태를 바꿀 일이 없다.

   storyEmbedder.embed(story)                     수백 ms ~ 수 초
   트랜잭션 밖. 커넥션을 잡지 않는다.

── tx  EmbeddingService.save() ───────────  수 ms
     product_vector upsert
     Inbox PROCESSED
── commit ──
```

`process()` 전체를 `@Transactional` 로 감싸면 배치 내내 커넥션을 붙잡게 됩니다.

### 4.2 왜 클래스를 나눴는가

`@Transactional` 은 Spring 이 만든 **프록시**가 호출을 가로채야 동작합니다. 같은 클래스 안의 메서드를 `this` 로 부르면 프록시를 거치지 않아 **어노테이션이 아무 일도 하지 않습니다.**

```java
// 안 됨 — 내부 호출이라 프록시를 타지 않는다
public void process(...) { markProcessed(event); }
@Transactional public void markProcessed(...) { ... }
```

그래서 트랜잭션처리를 별도 Bean으로 뺐습니다. `ProductEventHandler` → `EmbeddingService` 는 빈 사이의 호출이라 프록시를 탑니다.

### 4.3 저장소 기술이 달라도 한 트랜잭션

`product_vector` 는 `JdbcTemplate`, `recommendation_inbox` 는 JPA 입니다. 그래도 `save()` 안에서는 한 트랜잭션입니다. 같은 `DataSource` 를 쓰고 `JpaTransactionManager` 가 그 `DataSource` 를 노출하므로 `JdbcTemplate` 이 같은 커넥션에 참여합니다.

덕분에 **"벡터는 저장됐는데 Inbox 는 PENDING"** 인 상태가 생기지 않습니다. 그러면 다음 주기에 같은 임베딩을 또 하게 됩니다.

```java
@Transactional
public void save(Long eventId, Long productId, String story, Embedding embedding) {
    this.productVectorRepository.save(productId, story, embedding);   // 포트
    this.eventStore.markProcessed(eventId);                           // 포트
}
```

### 4.5 크래시 안전성

| 어긋난 상황            | 회복 경로                          | 결과          |
| ---------------- | ------------------------------ | ----------- |
| 처리 중 인스턴스가 죽음    | 상태를 안 바꿨으므로 `PENDING` 그대로 → 재조회 | 정상          |
| 커밋 전에 중단         | 위와 동일. 저장된 story 가 없어 다시 임베딩   | 정상          |
| 커밋 도중 실패         | 롤백. 벡터도 상태도 안 바뀜 → 다음 주기에 재처리  | 정상          |
| 벡터만 외부에서 삭제됨     | 조회가 비어 다시 임베딩           | 정상 (비용만 중복) |

`upsert` 가 멱등하고 `story` 비교가 있어 재처리는 항상 같은 결과로 수렴합니다.

---

## 5. 설정

```yaml
# application-local.yaml
recommendation:
  embedding:
    model-name: ${OLLAMA_EMBEDDING_MODEL:bge-m3}
  vector:
    dimensions: 1024       # bge-m3 네이티브 차원
    initialize-schema: true
  inbox:
    batch-size: 100
    poll-interval-ms: 5000
    initial-delay-ms: 5000     # 기동 직후 첫 폴링까지
    max-retry-count: 3         # 소진하면 대기열 조회에서 빠진다
```

운영은 `max-retry-count: 5` 입니다.

`initial-delay-ms` 는 테스트에서도 씁니다. `common` 의 `SchedulingConfig` 가
`@EnableScheduling` 이라 `@SpringBootTest` 컨텍스트에서도 스케줄러가 살아 있어서,
테스트 클래스패스에서는 이 값을 크게 잡아 폴링이 돌지 않게 합니다.

### 5.1 모델 선택 

| | 로컬 | 운영 |
| --- | --- | --- |
| `spring.ai.model.embedding` | `ollama` | `openai` |
| 모델 | `bge-m3` (1024차원) | `text-embedding-3-small` (1536차원) |
| 키 | 불필요 | `OPENAI_API_KEY` |

모델명은 `recommendation.embedding.model-name` 한 곳에서 관리하고
`spring.ai.{ollama,openai}.embedding.model` 이 그 값을 참조합니다 —
`product_vector.model_name` 에 기록되는 값과 실제 호출 모델이 어긋나지 않게 하기 위함입니다.


### 5.2 `inbox.batch-size` / `poll-interval-ms`

한 번에 몇 건을 꺼내고, 배치가 끝난 뒤 얼마를 쉴지입니다.

1건당 임베딩이 0.5초라 **배치가 가득 차면(100건) 한 사이클이 45~50초**입니다. 콜드 스타트(4~10초)는 배치 전체가 아니라 모델이 내려간 뒤 첫 건에만 붙습니다.

주기는 5초지만 `fixedRate` 가 아니라 **`fixedDelay`** 이므로 배치가 끝난 뒤부터 5초를 셉니다. 
### 5.3 `embedding.model-name`

`product_vector.model_name` 에 기록되고 재임베딩 판단에도 쓰입니다. 실제 모델과 어긋나면 판단이 틀어지므로 이 한 곳에서 관리하고, `spring.ai.ollama.embedding.model` 이 이 값을 참조합니다.

### 5.4 `vector.dimensions` / `initialize-schema`

Hibernate 가 `vector` 타입을 몰라 `ddl-auto` 로는 테이블이 만들어지지 않습니다. 로컬은 `ProductVectorSchemaInitializer` 가 기동 시 만들고, 운영은 `sql/schema-product-vector.sql` 로 배포 전에 만듭니다.

차원이 환경마다 다르므로(로컬 1024 / 운영 1536) DDL 에 이 값이 들어갑니다.

---

## 6. 검증

### 6.1 준비

```bash
# DB / Ollama
docker compose -f ../common/docker/local_postgres/docker-compose.local.yaml up -d
docker compose -f ../common/docker/ollama/docker-compose.local.yaml up -d
docker exec ollama-local ollama pull bge-m3


# 더미 이벤트 적재 (상품 100개)
docker exec -i commerce-postgres psql -U root -d dear < sql/seed-product-inbox.sql


# 기동
./gradlew :recommendation-service:bootRun
```



### 6.2 실행 결과

시드 103건(PENDING) 기준입니다.

```text
PENDING 103 → 0
FAILED 0
product_vector 100건
```

이벤트는 103건인데 상품은 100개입니다. 나머지 3건은 같은 상품의 중복/변경 이벤트입니다.

| 케이스              | 기대               | 결과                                       |
| ---------------- | ---------------- | ---------------------------------------- |
| E1 동일 Story 재전송  | 재임베딩 안 함         | `상품 101 Story 변경 없음 → 재임베딩 건너뜀`          |
| E2 Story 변경      | 재임베딩 후 갱신        | 102 벡터가 새 Story 로 바뀜                     |
| E3 배치에 섞인 과거 이벤트 | 최신 Story 가 남음    | 103 벡터가 10:02 Story 로 남음 (정렬 덕분, 가드는 안 탐) |
| E4 `PROCESSED` 행 | 다시 집어가지 않음       | 대기열에 안 나옴                                |
| E5 중복 `event_id` | 적재 자체가 안 됨       | `ON CONFLICT` 로 무시                       |

무결성 확인:

```sql
-- 차원과 영벡터 (PGvector 바인딩이 제대로 됐는지)
SELECT count(*) AS rows,
       min(vector_dims(embedding)) AS min_dim,
       max(vector_dims(embedding)) AS max_dim,
       count(*) FILTER (WHERE vector_norm(embedding) = 0) AS zero_vectors
FROM product_vector;
--  100 | 1024 | 1024 | 0

SELECT status, count(*) FROM recommendation_inbox GROUP BY status;
SELECT product_id, left(story, 40), model_name, embedded_at FROM product_vector ORDER BY product_id;
```

실데이터(운영의 더미데이터 47건, 42~566자)로도 같은 흐름을 확인했습니다.

```text
Inbox 47건 → 처리 시작   (배치 10회, 측정 당시 batch-size 5. 현재 설정은 100)
임베딩 완료 47건 / ERROR 0
recommendation_inbox  PENDING 47 → PROCESSED 47
product_vector        실데이터 벡터 47건 (평균 213자)
```

### 6.3 자가 복구 검증

벡터만 지우고 해당 이벤트를 다시 대기열에 올립니다.

```sql
DELETE FROM product_vector WHERE product_id IN (205, 601, 1001);
UPDATE recommendation_inbox SET status = 'PENDING' WHERE aggregate_id IN (205, 601, 1001);
```

기대 동작 — 건너뛰지 않고 다시 임베딩합니다.

```text
상품 205  임베딩 적재 완료 (임베딩 490ms, 저장 8ms, 1024차원)
상품 601  임베딩 적재 완료 (임베딩 473ms, 저장 7ms, 1024차원)
상품 1001 임베딩 적재 완료 (임베딩 493ms, 저장 9ms, 1024차원)

product_vector 97 → 100
"Story 변경 없음 → 재임베딩 건너뜀" 로그는 늘지 않음
```

### 6.4 순서 역전 가드 검증

이미 처리된 상품(101)에 더 이른 `occurred_at` 을 가진 이벤트를 넣습니다. `sql/seed-product-inbox.sql` 3번 섹션에 주석으로 들어 있습니다.

```sql
INSERT INTO recommendation_inbox (event_id, event_type, aggregate_id, occurred_at, payload, status)
VALUES (9100000005, 'PRODUCT_STORY_CHANGED', 101,
        TIMESTAMP '2026-08-24 08:00:00',
        jsonb_build_object('productId', 101, 'story', '뒤늦게 도착한 낡은 Story. 반영되면 안 된다.'),
        'PENDING');
```

기대 동작:

```text
로그   상품 101 순서 역전 이벤트 건너뜀 (occurredAt=2026-08-24T08:00)
상태   PROCESSED
벡터   변화 없음
```

벡터가 그대로인지는 지문으로 확인합니다.

```sql
SELECT md5(embedding::text) FROM product_vector WHERE product_id = 101;
```



