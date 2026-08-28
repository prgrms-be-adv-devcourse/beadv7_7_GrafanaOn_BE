# 임베딩 파이프라인

Inbox 에 적재된 상품 Story 이벤트가 `pgvector` 에 벡터로 쌓이기까지의 **현재 구현**을 정리한 문서입니다.

왜 그렇게 정했는지는 [rec_recommendation.md](./rec_recommendation.md), 유사 상품 조회는 [rec_similar.md](./rec_similar.md) 에 있습니다.

| 항목     | 현재 상태                                                    |
| ------ | -------------------------------------------------------- |
| 이벤트 유입 | commerce-service Outbox → Relay → `POST /internal/recommendations/product-events` |
| 처리 방식  | `@Scheduled` 폴링 (5초 간격, 5건씩). **선점(claim) 없음**                  |
| 임베딩    | Ollama `bge-m3` (1024차원). 상품 1개당 벡터 1개                    |
| 저장     | `product_vector` (직접 정의한 테이블, `JdbcTemplate`)             |
| 활성 조건  | `recommendation.pipeline.enabled=true` (로컬만)              |
| 검증 데이터 | `sql/seed-product-inbox.sql` — 상품 100건 + 분기 확인용 이벤트 5건        |

---

## 1. 전체 흐름

```text
recommendation_inbox (PENDING)
        │
        │ SpringEmbeddingScheduler — 5초마다 폴링 (fixedDelay)
        ▼
ProductEventStore.findPending(5)
        │  occurred_at 오름차순 5건. 선점하지 않는다
        ▼
ProductEventHandler.process()   [트랜잭션 없음]
        │
        ├─ 1. payload 파싱 ──────────── 실패 → FAILED
        │
        ├─ 2. 순서 역전 검사 ─────────── 낡은 이벤트 → PROCESSED (임베딩 안 함)
        │
        ├─ 3. 재임베딩 필요 여부 ──────── 저장된 story 와 동일 → PROCESSED (임베딩 안 함)
        │
        ├─ 4. ProductEmbedder.embed(story)  ← 가장 느린 구간. 트랜잭션 밖이라 커넥션을 잡지 않는다
        │                                        실패 → FAILED
        │
        └─ 5. EmbeddingService.save()   [트랜잭션]
                 ├─ product_vector   (INSERT ... ON CONFLICT DO UPDATE)
                 └─ Inbox PROCESSED
                                                 실패 → FAILED
```

> ⚠️ **선점(claim)은 아직 구현하지 않았습니다.**
> `PROCESSING` 상태도 `claimed_at` 컬럼도 없습니다. 처리 중인 이벤트도 DB 에서는 여전히
> `PENDING` 이므로, 배치가 겹치면 같은 이벤트를 두 번 임베딩합니다.
> 지금은 `fixedDelay` 폴링이 한 인스턴스 안에서 배치를 겹치지 않게 하는 것에 기대고 있고,
> **인스턴스가 여러 대면 중복 처리가 일어납니다.**
> 도입 계획은 [8장](#8-현재-한계)에 있습니다.

---

## 2. 구성 요소

| 파일                                                        | 역할                                            |
| --------------------------------------------------------- | --------------------------------------------- |
| `presentation/InternalRecommendationController`            | 수신 엔드포인트. Relay 가 부른다                          |
| `presentation/dto/request/ProductEventRelayRequest`        | 수신 요청 1건 → `RecommendationInbox`                |
| `infrastructure/scheduler/SpringEmbeddingScheduler`        | 폴링 진입점. 5초마다 배치 조회 → 1건씩 넘김                    |
| `application/handler/ProductEventHandler`                  | 이벤트 1건의 처리 흐름 전체. **트랜잭션 없음**                  |
| `application/EmbeddingService`                       | 쓰기 확정. **벡터 + Inbox 상태를 한 트랜잭션으로**             |
| `infrastructure/inbox/InboxService`                        | 수신 이벤트 적재. 중복은 DB 가 버린다                        |
| `application/dto/ProductPayload`                           | `{ productId, story }` 파싱 결과                   |
| `application/dto/ProductEvent`                             | 수신·처리 공용 이벤트 DTO. **Inbox 를 모른다**              |
| `infrastructure/inbox/InboxEventType`                      | `PRODUCT_UPDATED` / `PRODUCT_DELETED`. 인프라 밖으로 안 나간다 |
| `application/port/ProductEventStore`                       | 이벤트 저장소 **포트**. 쓰는 쪽이 소유한다                     |
| `domain/model/Embedding`                                   | 임베딩 결과 (`modelName` + 벡터). 불투명한 값              |
| `domain/repository/ProductEmbedder`                          | 임베딩 **포트**. Spring AI 를 모른다                    |
| `domain/repository/ProductVectorRepository`                | 벡터 저장소 **포트**                                  |
| `infrastructure/inbox/RecommendationInbox`                 | Inbox 행 **JPA 엔티티**. 컬럼 매핑과 상태 전이만 가짐            |
| `infrastructure/inbox/InboxStatus`                         | `PENDING` / `PROCESSED` / `FAILED`. 인프라 밖으로 안 나간다 |
| `infrastructure/inbox/RecommendationInboxAdapter`          | `ProductEventStore` 구현. 엔티티 ↔ `ProductEvent` 매핑 |
| `infrastructure/inbox/RecommendationInboxJpaRepository`    | Spring Data. 멱등 INSERT(네이티브) / 파생 조회 / JPQL    |
| `infrastructure/ai/SpringAiProductEmbedder`                  | 포트의 Spring AI 구현. **Spring AI 를 아는 유일한 클래스**   |
| `infrastructure/persistence/ProductVectorJdbcAdapter`      | `product_vector` upsert / 조회 (`JdbcTemplate`)  |
| `infrastructure/persistence/ProductVectorSchemaInitializer`| 기동 시 `product_vector` 테이블 생성                   |

테스트 24건 — `presentation/InternalRecommendationControllerTest` (요청 검증 4),
`infrastructure/inbox/InboxServiceTest` (멱등성 6), `infrastructure/inbox/InboxServiceConcurrencyTest` (동시 재전송 1),
`application/ProductEventHandlerTest` (분기 6), `application/dto/ProductEventTest` (4),
`infrastructure/inbox/RecommendationInboxTest` (상태 전이 3)

### 2.1 Inbox 는 인프라입니다

`recommendation_inbox` 와 관련된 것은 전부 `infrastructure/inbox` 에 있습니다. 도메인이 아닙니다.

판단 기준은 하나였습니다 — **전송 방식을 바꾸면 이 개념이 사라지는가.**

Inbox 는 HTTP 폴링이 at-least-once 이기 때문에 존재합니다. Kafka 로 바꿔 컨슈머 오프셋에 맡기면 이 테이블은 통째로 없어지고, 그래도 **추천의 도메인 규칙은 하나도 바뀌지 않습니다.** `event_id`, `aggregate_type`, `payload`, `retry_count` 는 발신 측 Outbox 행을 그대로 비추는 전송 필드일 뿐입니다.

이 서비스의 도메인 개념은 **Story, 임베딩(벡터), 유사도** 셋입니다. 그래서 `domain` 에는 그것만 남습니다.

```text
domain/model/       Embedding · RecommendationItem
domain/repository/  ProductVectorRepository · ProductEmbedder
```

### 2.2 그래서 DTO 로 끊습니다

Inbox 가 인프라라면 **`application` 도 Inbox 를 몰라야 합니다.** 파이프라인은 "처리할 이벤트" 를 다룰 뿐이고, 그게 인박스 테이블 행인지 Kafka 레코드인지는 알 필요가 없습니다.

```text
infrastructure/inbox/RecommendationInbox   ──어댑터의 toEvent()──▶   application/dto/ProductEvent
  id, status, retry_count, last_error,                   eventId, aggregateType, aggregateId,
  InboxEventType, ...(인박스 배관)                        eventType(String), payload(원문), occurredAt
```

포트는 **쓰는 쪽이 소유합니다.** `application/port/ProductEventStore` 가 필요한 것만 선언하고 `infrastructure/inbox/RecommendationInboxAdapter` 가 구현합니다.

```java
List<ProductEvent> findPending(int limit);
Optional<LocalDateTime> findLatestProcessedOccurredAt(String aggregateId);
void markProcessed(Long eventId);
void markFailed(Long eventId, String reason);
```

**시그니처에 `InboxStatus` 도 엔티티도 없는 것이 핵심입니다.** `findLatestOccurredAt(id, PROCESSED)` 였다면 호출자가 인박스 상태 값을 알아야 했지만 `findLatestProcessedOccurredAt(id)` 는 그럴 필요가 없고, 상태를 바꿀 때도 엔티티가 아니라 식별자만 넘깁니다([4.3](#43-저장소-기술이-달라도-한-트랜잭션)).

식별자는 인박스 행 PK 가 아니라 **발신 측이 부여한 `eventId`** 를 씁니다. `event_id` 에 UNIQUE 제약이 걸려 있어([2.5](#25-inbox-상태-전이)의 멱등성) 그 값만으로 행이 특정되고, 덕분에 인박스 내부 PK 가 `application` 으로 새지 않습니다. `markProcessed(eventId)` 는 어댑터에서 `findByEventId` 로 찾습니다.

### 2.3 다만 수신 경로는 이 경계를 타지 않습니다

포트를 타는 것은 **처리 경로뿐**입니다. 수신 경로는 인박스 엔티티를 직접 다룹니다.

```text
처리: 인박스 → ProductEventStore.findPending() → ProductEvent → ProductEventHandler → 벡터
                     └ 포트 경유. application 은 인박스를 모른다

수신: Relay → ProductEventRelayRequest.toInbox() → RecommendationInbox → InboxService → JPA
                     └ 포트를 우회. presentation 이 인박스 엔티티를 직접 만든다
```

그래서 의존 방향이 이렇습니다.

```text
[domain]         → domain
[application]    → application, domain              ← 인프라 0
[infrastructure] → application, domain
[presentation]   → application, domain, infrastructure 3   ← 여기만 바깥으로
```

`presentation → infrastructure` 3 건은 전부 수신 경로입니다.

| 파일 | 참조 |
| --- | --- |
| `ProductEventRelayRequest` | `RecommendationInbox`, `InboxEventType` |
| `InternalRecommendationController` | `InboxService` |

`domain` 에 `jakarta.persistence` / `org.hibernate` / `org.springframework` 가 들어오지 않고, `application` 에도 JPA 와 Spring Data 가 들어오지 않는다는 점은 유지됩니다. 갈라진 것은 **수신 경로 하나**입니다.

포트에 적재용 `append` 를 두지 않는 이유가 이것입니다. 두면 적재 경로가 둘로 보이는데,
실제로 쓰이는 것은 `InboxService` 쪽뿐이라 나중에 갈라질 수 있습니다.
**인박스 적재는 `RecommendationInboxJpaRepository.insertIgnoringDuplicate` 한 곳입니다.**

수신도 포트를 타게 하려면 `InboxService` 를 `application` 으로 올리고
`ProductEventRelayRequest` 가 엔티티 대신 `ProductEvent` 를 만들면 됩니다. 다만 그 경로에는
"배치 하나가 원자 단위" 말고는 정책이 없고, 멱등성은 DB 의 `ON CONFLICT` 가 맡고 있어
포트를 태워도 판단이 늘지 않습니다.

### 2.4 열거형은 인프라에 둡니다

`InboxEventType`(`PRODUCT_UPDATED` / `PRODUCT_DELETED`)과 `InboxStatus` 둘 다 `infrastructure/inbox` 에 있습니다. 인박스 컬럼과 이름이 일치하는 편이 읽기 쉽기 때문입니다.

두 경로가 이 열거형을 다르게 다룹니다.

```text
수신: ProductEventRelayRequest.eventType : InboxEventType   ← 열거형 그대로
처리: ProductEvent.eventType            : String            ← 문자열
```

**처리 쪽이 `String` 인 이유**는 `ProductEvent` 가 포트를 건너 `application` 으로 가기 때문입니다. 열거형을 그대로 실으면 `application` 이 인프라를 보게 됩니다. 어차피 `payload` 도 파싱하지 않은 원문 JSON 문자열로 들고 있으므로 `eventType` 만 타입이어야 할 이유가 없고, 되돌리는 것은 어댑터의 일입니다.

```java
inbox.getEventType().name()   // 어댑터: 엔티티 → ProductEvent
```

어댑터가 "삭제 이벤트인가" 까지 판정해서 `boolean` 으로 넘기는 방법도 있지만 그렇게 하지 않았습니다. 변환은 변환만 하고, 그 값으로 무엇을 판단할지는 쓰는 쪽이 정합니다. 삭제 여부는 `ProductEvent.isDeleted()` 가 봅니다.

**수신 쪽이 열거형인 이유**는 [2.3](#23-다만-수신-경로는-이-경계를-타지-않습니다) 과 같습니다. 그 경로는 어차피 인박스 엔티티를 직접 만들기 때문에, 문자열로 받았다가 되돌릴 이유가 없습니다.

> ⚠️ **모르는 이벤트 종류가 오면 500 이 나갑니다.**
> Jackson 이 열거형 역직렬화에서 먼저 터지고, 그 `HttpMessageNotReadableException` 을
> `CommonExceptionHandler` 가 따로 잡지 않아 catch-all 로 떨어집니다.
> Relay 입장에서 500 은 "재시도하라" 는 뜻이라 **버려야 할 배치를 계속 다시 보냅니다.**
> 공통 핸들러에 그 예외를 추가하거나, 문자열로 받고
> `@Pattern(regexp = "PRODUCT_UPDATED|PRODUCT_DELETED")` 을 걸면 400 으로 끊깁니다.

### 2.5 Inbox 상태 전이

`InboxStatus` 는 세 값입니다.

```text
  PENDING ──┬──▶ PROCESSED   임베딩 완료 / 소비만 하고 종료
            │
            └──▶ FAILED      payload 파싱 실패 / 임베딩 실패
```

되돌아오는 전이가 없습니다. `FAILED` 는 종착지라 다시 시도하지 않고, 처리 중인 이벤트는 DB 에서 여전히 `PENDING` 입니다.

엔티티의 `markAsFailed(reason)` 이 `retry_count` 를 올리고 `last_error` 에 사유를 남기지만, **그 값을 보고 다시 집어가는 코드는 없습니다.** 지금은 원인 추적용 기록입니다.

> ⚠️ **선점 상태가 없다는 것이 이 설계의 가장 큰 구멍입니다.**
> "이 이벤트는 누군가 가져갔다" 를 표시할 자리가 없어, 배치가 겹치면 같은 이벤트를 두 번 임베딩합니다.
> 한 인스턴스 안에서는 `fixedDelay` 가 겹침을 막아주지만 **인스턴스가 여러 대면 막을 방법이 없습니다.**
> 도입하려면 `PROCESSING` 상태와 `claimed_at` 컬럼이 필요하고, 그때는 스키마 변경이 따라옵니다 —
> Hibernate 가 `@Enumerated(EnumType.STRING)` 컬럼에 만들어 둔 CHECK 제약을
> `ddl-auto=update` 가 고쳐주지 않아 런타임에 이렇게 막힙니다.
>
> ```text
> ERROR: new row for relation "recommendation_inbox"
>        violates check constraint "recommendation_inbox_status_check"
> ```
>
> 운영(`ddl-auto=validate`)은 CHECK 제약을 검사하지 않아 **기동은 통과하고 런타임에 터집니다.**

### 2.6 테이블 관리 방식이 둘로 나뉩니다

| 테이블                    | 관리 방식          | 이유                                |
| ---------------------- | -------------- | --------------------------------- |
| `recommendation_inbox` | JPA Entity     | 일반 컬럼만 사용. 포트 + 어댑터로 감쌈           |
| `product_vector`       | `JdbcTemplate` | Hibernate 에 `vector` 타입이 없어 매핑 불가 |

---

## 3. 단계별 동작

### 3.1 대기열 조회

꺼내기만 합니다. 잠그지도, 상태를 바꾸지도 않습니다.

```java
List<ProductEvent> pending = eventStore.findPending(batchSize);
```

Spring Data 파생 쿼리이고, 실제로 나가는 SQL 은 이렇습니다.

```sql
select ... from recommendation.recommendation_inbox ri1_0
 where ri1_0.status = ?
 order by ri1_0.occurred_at
 fetch first ? rows only
```

* `occurred_at ASC` 로 꺼내므로 **오래된 이벤트가 먼저** 처리됩니다. `idx_recommendation_inbox_status_occurred_at` 을 탑니다.
* 어댑터가 부르는 Spring Data 메서드 이름이 `findByStatusOrderByOccurredAtAsc` 입니다. **`OrderByOccurredAtAsc` 를 빼면** `ORDER BY` 없이 나가고 순서는 플래너 판단이 됩니다.
* 트랜잭션을 열지 않습니다. 조회는 그 자체로 끝나고, 쓰기는 `EmbeddingService` 가 건별로 짧은 트랜잭션을 엽니다.
* `fixedDelay` 라 이전 실행이 끝난 뒤에야 다음 실행이 시작됩니다. **한 인스턴스 안에서는** 배치가 겹치지 않습니다.
* 1건이 예외로 터져도 나머지는 계속 처리합니다.

> ⚠️ 선점하지 않으므로 **처리 중인 이벤트도 DB 에서는 `PENDING`** 입니다.
> 인스턴스가 여러 대면 같은 이벤트를 동시에 집어 중복 임베딩이 일어납니다. ([2.5](#25-inbox-상태-전이))

### 3.2 payload 파싱

Inbox 의 `payload` 는 `jsonb` 에 담긴 **원문 JSON** 입니다. 적재 시점에 역직렬화하지 않기 때문에 발행 측에 필드가 추가되어도 수신은 실패하지 않고, 파싱은 `ProductEventHandler.parse()` 에서 처음 일어납니다.

```java
record ProductPayload(Long productId, String story)
```

레코드 컴포넌트 이름이 곧 JSON 필드명입니다. `productId` 가 없거나 `story` 가 비어 있으면 `FAILED` 로 남깁니다.

파싱을 어댑터로 내려서 `application` 에서 `ObjectMapper` 를 걷어내는 방법도 만들어 봤지만 되돌렸습니다. `ProductEvent` 가 수신·처리 공용이라 적재 경로는 원문 String 이, 처리 경로는 파싱 결과가 필요해져서 **필드가 둘로 늘고 경로마다 한쪽이 `null`** 이 됐습니다. 얻는 것이 `ObjectMapper` 의 소속뿐이라 비용이 더 컸습니다.

파싱과 "실패하면 `FAILED`" 라는 정책이 한 클래스에 붙어 있는 편이 읽기에도 낫습니다. 어댑터가 파싱하면 판정은 핸들러에 남아 둘이 떨어집니다.

### 3.3 순서 역전 검사

```java
LocalDateTime lastProcessed = eventStore.findLatestProcessedOccurredAt(event.aggregateId()).orElse(null);
if (event.isStaleAgainst(lastProcessed)) → PROCESSED 로 소비만 하고 종료
```

같은 상품에서 **이미 처리한 이벤트보다 먼저 발생한** 이벤트라면 벡터를 건드리지 않습니다. 늦게 도착한 과거 Story 가 최신 Story 를 덮어쓰는 것을 막습니다.

판정은 `ProductEvent.isStaleAgainst` 가 합니다. 인박스 행이 아니라 **이벤트에 대한 규칙**이라 DTO 가 들고 있습니다 ([2.2](#22-그래서-dto-로-끊습니다)).

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

### 3.5 임베딩과 적재

```java
Embedding embedding = storyEmbedder.embed(story);                            // 트랜잭션 밖. 수백 ms ~ 수 초
commitService.save(event.id(), productId, story, embedding);                 // 트랜잭션 안. 벡터 + 상태
```

`ProductEmbedder`(임베딩)와 `ProductVectorRepository`(적재)가 나뉘어 있는 이유가 이것입니다. 모델 호출을 트랜잭션 밖에 두어야 그 시간 동안 DB 커넥션을 붙잡지 않습니다. ([4장](#4-트랜잭션-경계와-동시성))

* `EmbeddingModel` 을 직접 호출합니다. 임베딩 시간과 저장 시간을 따로 측정할 수 있습니다.
* `product_id` 가 PK 이므로 `INSERT ... ON CONFLICT (product_id) DO UPDATE` 로 갱신됩니다.
* 벡터는 `com.pgvector:pgvector` 의 `PGvector` 로 바인딩합니다. `PGobject` 구현체라 드라이버가 그대로 처리하므로 문자열 조립이나 `::vector` 캐스팅이 없습니다.

**청킹하지 않습니다.** 상품 1개 = 벡터 1개입니다. Spring AI 의 `TextSplitter` 는 새 `Document` 를 만들면서 `id()` 를 넘기지 않아 `productId` 가 사라집니다. 청킹을 도입하려면 "상품 1개 = 벡터 1개" 전제부터 다시 잡아야 합니다.

### 3.6 상태 갱신

`PROCESSED` 로 바꿉니다. 별도의 이력 테이블은 없습니다.

벡터 적재와 **같은 트랜잭션**에서 일어납니다. 호출자는 `markProcessed(id)` 로 식별자만 넘기고, 어댑터가 그 트랜잭션 안에서 엔티티를 로드해 상태를 바꿉니다. 변경 감지로 커밋 시점에 UPDATE 가 나갑니다.

---

## 4. 트랜잭션 경계와 동시성

### 4.1 경계를 어디에 두는가

한 이벤트를 처리하는 동안 트랜잭션은 **끝에 한 번만** 열립니다. 그 앞에 임베딩이 있습니다.

```text
   eventStore.findPending(5)                      트랜잭션 없음
   조회는 그 자체로 끝난다. 선점하지 않으므로 상태를 바꿀 일이 없다.

   storyEmbedder.embed(story)                     수백 ms ~ 수 초
   트랜잭션 밖. 커넥션을 잡지 않는다.

── tx  EmbeddingService.save() ───────────  수 ms
     product_vector upsert
     Inbox PROCESSED
── commit ──
```

느린 구간을 트랜잭션 밖으로 밀어내는 것이 이 구조의 목적입니다. `process()` 전체를 `@Transactional` 로 감싸면 배치 내내 커넥션을 붙잡게 됩니다.

### 4.2 왜 클래스를 나눴는가

`@Transactional` 은 Spring 이 만든 **프록시**가 호출을 가로채야 동작합니다. 같은 클래스 안의 메서드를 `this` 로 부르면 프록시를 거치지 않아 **어노테이션이 아무 일도 하지 않습니다.**

```java
// 안 됨 — 내부 호출이라 프록시를 타지 않는다
public void process(...) { markProcessed(event); }
@Transactional public void markProcessed(...) { ... }
```

그래서 쓰기 확정을 별도 빈으로 뺐습니다. `ProductEventHandler` → `EmbeddingService` 는 빈 사이의 호출이라 프록시를 탑니다.

`ProductVectorJdbcAdapter` 에는 `@Transactional` 을 붙이지 않습니다. `save` 는 문장 하나뿐이라 자체 경계가 필요 없고, 묶여야 할 상대(Inbox 상태)는 그 클래스 밖에 있습니다.

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

**둘 다 포트입니다.** 이 클래스는 `JdbcTemplate` 도 JPA 엔티티도 모릅니다. `markProcessed(id)` 는 어댑터 안에서 엔티티를 로드해 상태를 바꾸고, 변경 감지로 이 트랜잭션이 커밋될 때 함께 반영됩니다.

한동안 이 메서드가 `RecommendationInbox` 엔티티를 인자로 받았습니다. "원자성을 지키려면 application 이 엔티티를 들고 있어야 한다" 고 생각했기 때문인데, **틀린 판단이었습니다.** 트랜잭션에 필요한 것은 두 포트가 같은 트랜잭션에 참여하는 것뿐이고, 식별자만 넘겨도 원자성은 그대로입니다. 엔티티를 넘기던 시절에는 `application` 이 `infrastructure.inbox` 를 참조해야 했습니다.

### 4.4 Scale-out — 아직 안전하지 않습니다

**인스턴스가 여러 대면 같은 행을 두 번 집습니다.** 조회가 잠그지도 상태를 바꾸지도 않아서, 두 인스턴스의 폴링이 같은 `PENDING` 행을 함께 가져갑니다. 지금 안전한 것은 한 인스턴스 안에서 `fixedDelay` 가 배치를 겹치지 않게 해줄 때뿐입니다.

도입한다면 `FOR UPDATE SKIP LOCKED` 로 고른 행을 잠그고 `PROCESSING` 으로 바꾸는 선점 트랜잭션이 필요합니다. 잠금만으로는 부족합니다 — 잠금은 그 트랜잭션이 커밋되는 순간 풀리는데 임베딩은 그 뒤에 일어나므로, 풀린 뒤에도 다시 집히지 않으려면 상태가 남아 있어야 합니다.

ShedLock 같은 배타 잠금은 쓰지 않을 생각입니다. 그러면 **한 번에 한 인스턴스만** 일하게 됩니다. `SKIP LOCKED` 는 인스턴스들이 서로 다른 행을 나눠 가지므로 대수만큼 처리량이 올라갑니다.

### 4.5 크래시 안전성

| 어긋난 상황            | 회복 경로                          | 결과          |
| ---------------- | ------------------------------ | ----------- |
| 처리 중 인스턴스가 죽음    | 상태를 안 바꿨으므로 `PENDING` 그대로 → 재조회 | 정상          |
| 커밋 전에 중단         | 위와 동일. 저장된 story 가 없어 다시 임베딩   | 정상          |
| 커밋 도중 실패         | 롤백. 벡터도 상태도 안 바뀜 → 다음 주기에 재처리  | 정상          |
| 벡터만 외부에서 삭제됨     | 조회가 비어 다시 임베딩 (3.4)            | 정상 (비용만 중복) |

**선점하지 않는 것이 크래시 안전성에는 유리하게 작용합니다.** 상태를 미리 바꾸지 않으니 되돌릴 것도 없고, 죽은 인스턴스가 붙잡고 있던 이벤트가 방치될 일도 없습니다. 선점을 도입하면 이 표의 첫 줄이 "회수가 없으면 영영 처리되지 않음" 으로 바뀌므로, 방치분 회수 스윕이 함께 필요해집니다.

`upsert` 가 멱등하고 `story` 비교(3.4)가 있어 재처리는 항상 같은 결과로 수렴합니다.

마지막 줄은 실제로 겪은 사고입니다. 이력 테이블을 따로 두던 시절에는 이 상황에서 복구되지 않았고, 그래서 이력 테이블을 없앴습니다.

### 4.6 남는 틈

같은 상품의 이벤트 두 건이 서로 다른 인스턴스에 **동시에** 집히면, 둘 다 상대를 `PROCESSED` 로 보지 못해 순서 역전 검사(3.3)를 통과합니다. 이때는 나중에 커밋한 쪽이 남습니다. 대기열이 `occurred_at` 순이라 한 인스턴스 안에서는 발생하지 않고, 뒤이어 오는 이벤트가 최신 Story 로 덮으므로 수렴합니다. 상품 단위 직렬화가 필요해지면 `aggregate_id` 를 선점 단위로 잡아야 합니다.

---

## 5. 설정

```yaml
recommendation:
  pipeline:
    enabled: true          # 파이프라인 빈의 생성 조건
  embedding:
    model-name: ${OLLAMA_EMBEDDING_MODEL:bge-m3}
  vector:
    dimensions: 1024       # bge-m3 네이티브 차원
    initialize-schema: true
  inbox:
    batch-size: 5
    poll-interval-ms: 5000


```

### 5.1 `pipeline.enabled`

파이프라인 빈 전부가 이 값에 걸려 있습니다. `SpringAiProductEmbedder`, `ProductEventHandler`, `EmbeddingService`, `SpringEmbeddingScheduler`.

**운영은 아직 false 입니다.** OpenAI 를 붙이기 전이라 EmbeddingModel 이 없어서, 켜면 기동 자체가 실패합니다. 켜는 순서는 `application-prod.yaml` 주석에 있습니다.

`ProductVectorSchemaInitializer` 는 `vector.initialize-schema` 에 걸려 있습니다. 운영은 `false` 라 기동 시 `CREATE EXTENSION` 을 시도하지 않습니다. superuser 권한이 필요한 DDL 이라 배포 전 스크립트로 분리했습니다.

### 5.2 `inbox.batch-size` / `poll-interval-ms`

한 번에 몇 건을 꺼내고, 배치가 끝난 뒤 얼마를 쉴지입니다.

1건당 임베딩이 웜 0.5초 / 콜드 8초라 배치 5건이면 최악 40초쯤 걸립니다. 주기는 5초지만 `fixedRate` 가 아니라 **`fixedDelay`** 이므로 배치가 끝난 뒤부터 5초를 셉니다. `fixedRate` 면 밀린 실행이 곧바로 이어져 배치가 겹치고, 선점이 없는 지금은 그대로 중복 임베딩이 됩니다.

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

실데이터(운영 덤프의 스토리 47건, 42~566자)로도 같은 흐름을 확인했습니다.

```text
Inbox 47건 선점 → 처리 시작   (배치 10회, batch-size 5)
임베딩 완료 47건 / ERROR 0
recommendation_inbox  PENDING 47 → PROCESSED 47
product_vector        실데이터 벡터 47건 (평균 213자)
```

시드는 합성 문장 100건이 전부 50자 안팎이라 길이 편차가 없습니다. 실데이터는 42~566자로 흩어져 있어 **긴 Story 에서 임베딩 시간이 늘어나는지**를 여기서 볼 수 있습니다.

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

### 6.5 유사 상품 조회

파이프라인이 만든 벡터로 [recommendation.md 7장](./recommendation.md#7-vector-search-방식)의 조회를 실행한 결과입니다.

```sql
SELECT s.product_id,
       round((s.embedding <=> b.embedding)::numeric, 4) AS distance,
       left(s.story, 26) AS story
FROM product_vector s, product_vector b
WHERE b.product_id = 101 AND s.product_id <> b.product_id
ORDER BY distance
LIMIT 5;
```

```text
 product_id | distance |            story
------------+----------+------------------------------
        103 |   0.3069 | 농구 동아리 시절 매일 신고 다니던 나이키 에어
        102 |   0.3513 | 졸업 선물로 받은 조던1 시카고입니다. 착용 흔
        104 |   0.3687 | 러닝을 시작하면서 산 뉴발란스 993입니다. 발
        201 |   0.3897 | 첫 월급으로 산 루이비통 스피디 가방입니다. 중
        605 |   0.4099 | 첫 출근용으로 산 겨울 패딩입니다. 따뜻해서 5
```

기준 상품이 운동화이고 상위 3건이 모두 운동화입니다. Threshold 를 정할 때 걸림돌이 되는 관찰은 [recommendation.md 13.1](./recommendation.md#131-더미-데이터-100건으로-미리-본-것) 에 정리했습니다.

### 6.6 선점 / 회수 검증 — 도입할 때를 위한 메모

> ⚠️ **아래는 아직 구현하지 않은 선점(claim)에 대한 것입니다.**
> `PROCESSING` 상태도 `claimed_at` 컬럼도 없으므로 지금은 그대로 실행되지 않습니다.
> 도입할 때 쓰려고 남겨 둔 검증 절차입니다. ([8장](#8-현재-한계))

선점이 실제로 중복을 막는지는 세션 두 개로 눈으로 확인할 수 있습니다. `psql` 의 `\!` 로 트랜잭션을 연 채 다른 세션을 띄웁니다.

```bash
docker exec -i commerce-postgres psql -U root -d dear <<'SQL'
BEGIN;
SELECT id, aggregate_id FROM recommendation_inbox
 WHERE status='PENDING' ORDER BY occurred_at, id LIMIT 3 FOR UPDATE SKIP LOCKED;

\! psql -U root -d dear -c "SELECT id FROM recommendation_inbox WHERE status='PENDING' ORDER BY occurred_at, id LIMIT 3 FOR UPDATE SKIP LOCKED;"

\! psql -U root -d dear -c "SET lock_timeout='2s'; SELECT id FROM recommendation_inbox WHERE status='PENDING' ORDER BY occurred_at, id LIMIT 3 FOR UPDATE;"
ROLLBACK;
SQL
```

```text
세션 A                     153 154 155
세션 B  (SKIP LOCKED)      156 157 158   ← 겹치지 않는다. 기다리지도 않는다
세션 B2 (SKIP LOCKED 없음)  ERROR: canceling statement due to lock timeout
                                  while locking tuple (8,1)
```

세 번째가 핵심입니다. `SKIP LOCKED` 가 없으면 **뒤에 온 인스턴스가 앞선 잠금이 풀릴 때까지 멈춰 섭니다.** 임베딩이 끝날 때까지가 아니라 선점 트랜잭션이 끝날 때까지지만, 인스턴스를 늘려도 처리량이 늘지 않는 구조가 됩니다.

회수는 `claimed_at` 을 과거로 돌려 재현합니다.

```sql
-- 죽은 인스턴스가 선점한 상황
UPDATE recommendation_inbox
   SET status='PROCESSING', claimed_at = now() - INTERVAL '10 minutes'
 WHERE event_id BETWEEN 9200000001 AND 9200000003;

-- releaseStaleClaims 와 같은 UPDATE
UPDATE recommendation_inbox
   SET status='PENDING', claimed_at=NULL
 WHERE status='PROCESSING' AND claimed_at < now() - INTERVAL '5 minutes';
```

```text
회수 전   PROCESSING 3 / PENDING 7
회수 후   PENDING 10
```

선점을 도입하면 회수 스윕이 주기적으로 같은 일을 하게 됩니다.

```text
WARN  PROCESSING 으로 방치된 Inbox 3건을 PENDING 으로 되돌렸다 (기준 ...)
```

### 6.7 정리

```bash
docker exec -i commerce-postgres psql -U root -d dear < sql/cleanup-product-inbox.sql
```

`product_vector` → `recommendation_inbox` 순으로 지웁니다. 삭제 대상 상품을 Inbox 에서 뽑아내므로 순서가 중요합니다.

---

## 7. 소요 시간 실측

상품 100건 실행에서 102건 측정한 결과입니다. (103건 처리 중 1건 스킵)

| 구간          | 중앙값       | 평균     |
| ----------- | --------- | ------ |
| 임베딩 (`EmbeddingModel.embed`) | **493 ms** | 568 ms |
| 저장 (upsert) | **7 ms**  | 9 ms   |

`EmbeddingModel` 을 직접 호출하게 되면서 두 구간이 분리 측정 가능해졌고, **병목이 전적으로 모델 호출** 이라는 것이 드러났습니다. DB 쓰기는 사실상 공짜입니다.

첫 호출은 Ollama 가 모델을 메모리에 올리느라 4~10초가 걸립니다. `OLLAMA_KEEP_ALIVE=30m` 이라 그 사이에는 다시 로딩되지 않습니다.

> `application-local.yaml` 의 `batch-size` 주석에 아직 "1건당 임베딩이 약 8초" 가 남아 있습니다.
> 8초는 콜드 스타트 값이고 웜은 0.5초입니다.

### 7.1 배치 크기

`fixedDelay` 5초 + 건당 0.45초라, batch-size 5 로는 **한 사이클 7.3초 중 5초를 쉽니다.**

| batch | 간격 | 처리량      | 100건    |
| ----- | -- | -------- | ------- |
| 5 (현재) | 5초 | 0.69건/초  | 2분 30초  |
| 20    | 2초 | 1.82건/초  | 55초     |
| 100   | 5초 | 2.0건/초   | 50초     |
| —     | —  | 2.2건/초 (천장) | 45초  |

큐가 비어 있는 평상시에는 batch-size 가 영향을 주지 않습니다(1건 있으면 1건만 가져옴). **대량 적재에서만 의미가 있습니다.**

천장 2.2건/초는 이벤트마다 임베딩을 1회씩 호출하기 때문입니다. Ollama `/api/embed` 는 배열 입력을 받으므로 여러 건을 한 요청으로 묶으면 더 올릴 수 있습니다.

---

## 8. 현재 한계

* **선점(claim) 없음** — 처리 중인 이벤트도 DB 에서는 `PENDING` 입니다. 한 인스턴스 안에서는 `fixedDelay` 가 겹침을 막지만, **인스턴스가 여러 대면 같은 이벤트를 두 번 임베딩합니다.** `PROCESSING` 상태 + `claimed_at` + `FOR UPDATE SKIP LOCKED` + 방치분 회수가 필요합니다. ([2.5](#25-inbox-상태-전이))
* **`FAILED` 재시도 없음** — `retry_count` 와 `last_error` 는 기록되지만 그 값을 보고 다시 집어가는 코드가 없습니다. `FAILED` 는 종착지입니다.
* **백필 경로 없음** — 재임베딩은 새 이벤트가 와야 일어납니다. 모델을 바꿔 테이블을 재구축하거나 운영에서 파이프라인을 처음 켜면 벡터가 0인데 채울 수단이 없습니다. ([rec_similar.md 8.2](./rec_similar.md#82-백필-경로가-없습니다))
* **상품 단위 직렬화 없음** — 같은 상품의 이벤트 두 건이 같은 배치에 들어오면 순서 역전 검사가 걸러주지만, 서로 다른 인스턴스에 동시에 집히면 늦게 커밋한 쪽이 남습니다. ([4.6](#46-남는-틈))
* **story 길이 상한 없음** — 임베딩 모델에는 입력 토큰 상한이 있는데 발신 측에 길이 제약이 없습니다. 넘으면 `FAILED` 로 남습니다.
