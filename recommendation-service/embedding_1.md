# 임베딩 파이프라인

Inbox 에 적재된 상품 Story 이벤트가 `pgvector` 에 벡터로 쌓이기까지의 **현재 구현**을 정리한 문서입니다.

왜 그렇게 정했는지는 [recommendation.md](./recommendation.md), 성능 측정 방법은 [bench/README.md](./bench/README.md) 에 있습니다.

| 항목     | 현재 상태                                                    |
| ------ | -------------------------------------------------------- |
| 이벤트 유입 | 아직 없음. `sql/seed-product-inbox.sql` 로 Inbox 를 채운다         |
| 처리 방식  | `@Scheduled` 폴링 (5초 간격, 5건씩)                             |
| 임베딩    | Ollama `bge-m3` (1024차원). 상품 1개당 벡터 1개                    |
| 저장     | `product_vector` (직접 정의한 테이블, `JdbcTemplate`)             |
| 활성 조건  | `recommendation.pipeline.enabled=true` (로컬만)              |
| 검증 데이터 | 상품 100건 + 분기 확인용 이벤트 5건                                  |

---

## 1. 전체 흐름

```text
recommendation_inbox (PENDING)
        │
        │ SpringEmbeddingScheduler — 5초마다 occurred_at 순으로 5건
        ▼
EmbeddingScheduller
        │
        ├─ 1. payload 파싱 ──────────── 실패 → FAILED
        │
        ├─ 2. 순서 역전 검사 ─────────── 낡은 이벤트 → PROCESSED (임베딩 안 함)
        │
        ├─ 3. 재임베딩 필요 여부 ──────── 저장된 story 와 동일 → PROCESSED (임베딩 안 함)
        │
        ├─ 4. ProductStoryEmbeddingService
        │        ├─ EmbeddingModel.embed(story)   ← 가장 느린 구간
        │        └─ ProductVectorRepository.upsert()
        │                 │                          실패 → FAILED
        │                 ▼
        │            product_vector   (INSERT ... ON CONFLICT DO UPDATE)
        │
        └─ 5. PROCESSED
```

---

## 2. 구성 요소

| 파일                                                     | 역할                                        |
| ------------------------------------------------------ | ----------------------------------------- |
| `infrastructure/scheduler/SpringEmbeddingScheduler`     | PENDING 폴링, 배치 단위로 꺼내 1건씩 넘김              |
| `application/scheduller/EmbeddingScheduller`            | 이벤트 1건의 처리 흐름 전체                           |
| `application/dto/ProductPayload`                        | `{ productId, story }` 파싱 결과               |
| `infrastructure/ai/ProductStoryEmbeddingService`        | 임베딩 호출 + 적재, 재임베딩 필요 여부 판단                |
| `infrastructure/ai/db/ProductVectorRepository`          | `product_vector` upsert / 조회 (`JdbcTemplate`) |
| `infrastructure/ai/db/ProductVectorSchemaInitializer`   | 기동 시 `product_vector` 테이블 생성               |
| `domain/model/RecommendationInbox`                      | 이벤트. 상태 전이와 순서 역전 판정을 가짐                  |
| `domain/repository/RecommendationInboxRepository`       | Inbox 저장소 **포트** (기술 비의존)                  |
| `infrastructure/persistence/RecommendationInboxAdapter` | 포트의 JPA 구현. `Pageable` 변환을 흡수              |
| `infrastructure/persistence/jpa/RecommendationInboxJpaRepository` | Spring Data 파생 쿼리 / JPQL         |

테스트: `application/EmbeddingSchedullerTest` (분기 6건)

### 2.1 테이블 관리 방식이 둘로 나뉩니다

| 테이블                    | 관리 방식          | 이유                                |
| ---------------------- | -------------- | --------------------------------- |
| `recommendation_inbox` | JPA Entity     | 일반 컬럼만 사용. 포트 + 어댑터로 감쌈           |
| `product_vector`       | `JdbcTemplate` | Hibernate 에 `vector` 타입이 없어 매핑 불가 |

---

## 3. 단계별 동작

### 3.1 대기열 조회 — `SpringEmbeddingScheduler`

```java
@Scheduled(fixedDelayString = "${recommendation.inbox.poll-interval-ms}")
inboxRepository.findByStatusOrderByOccurredAtAsc(InboxStatus.PENDING, batchSize)
```

* `occurred_at ASC` 로 꺼내므로 **오래된 이벤트가 먼저** 처리됩니다. `idx_recommedation_inbox_status_occurred_at` 을 탑니다.
* 포트는 `int limit` 만 받고, `PageRequest` 로 바꾸는 일은 `RecommendationInboxAdapter` 가 합니다.
* `fixedDelay` 라 이전 실행이 끝난 뒤에야 다음 실행이 시작됩니다. 배치가 겹치지 않습니다.
* 1건이 예외로 터져도 나머지는 계속 처리합니다.

### 3.2 payload 파싱

Inbox 의 `payload` 는 `jsonb` 에 담긴 **원문 JSON** 입니다. 적재 시점에 역직렬화하지 않기 때문에 발행 측에 필드가 추가되어도 수신은 실패하지 않고, 파싱은 여기서 처음 일어납니다.

```java
record ProductPayload(Long productId, String story)
```

레코드 컴포넌트 이름이 곧 JSON 필드명입니다. `productId` 가 없거나 `story` 가 비어 있으면 `FAILED` 로 남깁니다.

### 3.3 순서 역전 검사

```java
LocalDateTime lastProcessed = inboxRepository.findLatestOccurredAt(aggregateId, PROCESSED);
if (event.isStaleAgainst(lastProcessed)) → PROCESSED 로 소비만 하고 종료
```

같은 상품에서 **이미 처리한 이벤트보다 먼저 발생한** 이벤트라면 벡터를 건드리지 않습니다. 늦게 도착한 과거 Story 가 최신 Story 를 덮어쓰는 것을 막습니다.

> 대기열이 `occurred_at` 순이라 한 배치 안에서는 이 분기가 잘 타지 않습니다.
> 실제로 필요한 상황은 **이미 처리가 끝난 뒤에 과거 이벤트가 도착**하는 경우입니다. ([6.4](#64-순서-역전-가드-검증))

### 3.4 재임베딩 필요 여부

```java
embeddingService.isAlreadyEmbedded(productId, story)

// SELECT story FROM product_vector WHERE product_id = ? AND model_name = ?
//  → 이 값이 새 story 와 같으면 건너뛴다
```

`product_vector.story` 에는 임베딩에 넣은 텍스트가 그대로 들어 있습니다. 즉 **이 컬럼이 곧 "이 벡터를 만든 원문"** 이므로, 별도의 이력 테이블 없이 여기서 바로 판단합니다.

모델명을 함께 비교하는 이유는 모델이 바뀌면 벡터 공간이 달라져 Story 가 같아도 다시 임베딩해야 하기 때문입니다.

**진실의 출처가 하나** 이므로 벡터가 지워지면 조회 결과가 비고, 자연히 다시 임베딩합니다. 예전에 `product_embedding_log` 이력 테이블을 따로 두었을 때는 벡터만 지워진 상황에서 이력이 "이미 임베딩했다"고 답해 파이프라인이 영원히 건너뛰었습니다. ([6.3](#63-자가-복구-검증))

### 3.5 임베딩 적재

```java
float[] embedding = embeddingModel.embed(story);
productVectorRepository.upsert(productId, story, modelName, embedding);
```

* `EmbeddingModel` 을 직접 호출합니다. 임베딩 시간과 저장 시간을 따로 측정할 수 있습니다.
* `product_id` 가 PK 이므로 `INSERT ... ON CONFLICT (product_id) DO UPDATE` 로 갱신됩니다.
* 벡터는 `com.pgvector:pgvector` 의 `PGvector` 로 바인딩합니다. `PGobject` 구현체라 드라이버가 그대로 처리하므로 문자열 조립이나 `::vector` 캐스팅이 없습니다.

**청킹하지 않습니다.** 상품 1개 = 벡터 1개입니다. Spring AI 의 `TextSplitter` 는 새 `Document` 를 만들면서 `id()` 를 넘기지 않아 `productId` 가 사라집니다. 청킹을 도입하려면 "상품 1개 = 벡터 1개" 전제부터 다시 잡아야 합니다.

### 3.6 상태 갱신

`RecommendationInbox` 를 `PROCESSED` 로 바꾸고 저장합니다. 별도의 이력 테이블은 없습니다.

---
