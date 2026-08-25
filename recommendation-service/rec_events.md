## 상품 이벤트 전달
상품의 생성 / 수정 / 삭제 이벤트를 recommendation-service로 전달해 임베딩에 필요한 데이터를 전달합니다.


---

## 1. 구조

```
commerce-service                                    recommendation-service 
                                                                                          
  ProductService.createProduct/update/delete         InternalProductEventController       
    │  @Transactional                                  POST /internal/recommendation/…    
                          
    ├─ productOutboxAppender.append()                          ▼                          
    │     └─▶ product_outbox INSERT                  InboxService.saveProductEvents       
    └─ productEventPublisher.publish(event)            eventId 중복 체크, 유니크키 제약      
                    ▲                                                                   
         하나의 트랜잭션으로 커밋/롤백                            ▼                          
                                                      recommendation_inbox INSERT          
                                                        status = PENDING                   
  OutboxPollingScheduler  @Scheduled(5s)                                             
    · SENT 제외 조회 (inserted_at asc)                           ▼                          
    · RecommendationHttpClient 로 전송                 임베딩 배치 (#218이슈에서 구현)                 
    · 성공 markSent / 실패 markFailed                                                      
```

두 서비스는 **같은 DB 인스턴스(`dear`)를 쓰되 스키마로 분리**되어 있습니다.
서로의 테이블을 직접 읽지 않고 HTTP로만 통신합니다.

---

## 2. Outbox 패턴 도입

### 도입배경

상품 저장(DB 트랜잭션)과 recommendation으로의 이벤트 전달(HTTP)은 하나의 원자적 단위로 묶을 수 없습니다.
DB 트랜잭션이 커밋되어도 이벤트 발행은 보장할 수 없고, 반대로 이벤트를 보낸 뒤 트랜잭션이 롤백될 수도 있습니다.

Outbox는 **이벤트를 같은 DB 트랜잭션 안에서 테이블에 적재**해 이 간극을 없앱니다.
상품과 이벤트가 함께 커밋되거나 함께 사라지므로 유실이 발생하지 않습니다.

```java
// ProductService — 하나의 @Transactional 안
productRepository.save(product);                              // 상품
productOutboxAppender.append(id, PRODUCT_UPDATED, story);     // 이벤트 
```


### 이벤트로 전달하는 값 (payload)

임베딩에 필요한 값만 담습니다.

```json
{ "productId": 101, "story": "가죽이 길들여진 스니커즈" }
```

삭제 이벤트는 `story`가 없으므로 `null`이 들어갑니다.

---

## 3. 이벤트 전달 방식: Polling vs CDC

| | Polling Publisher (스케줄러) | Transaction Log Tailing (CDC) |
|---|---|---|
| 방식 | 폴링 주기마다 PENDING 조회 후 전달 | DB 트랜잭션 로그를 읽어 INSERT 즉시 감지 |
| 추가 인프라 | 없음 | Debezium  |
| 지연 | 폴링 주기만큼 | 실시간 |
| 구현 난이도 | 낮음 | 높음 |

postgreSQL은 논리적 복제(Logical Replication)와 Logical Decoding 기능을 제공하여 별도 인프라 없이도 CDC를 구현할 수 있습니다. 하지만  inbox에 적재된 데이터를 임베딩할 때 배치로 묶어 처리하는 편이 효율적이라
실시간 전달의 이점이 없습니다. 구현 난이도가 낮고 배치 처리에 잘 맞는 Polling 방식을 사용합니다.

---

## 4. Inbox 패턴 (수신 측)

Outbox패턴 도입 시 수신 측은 **at-least-once**와 멱등성을 보장해야 합니다. at-least-once는 중복 수신이 될 수 있다는 것으로, 수신측에서는 중복 데이터를 검증하여 제거해야 합니다.

- 발신 측 outbox 행의 `id`를 `event_id`로 저장하고 **UNIQUE 제약**을 겁니다.
- 적재 전에 이미 존재하는 `event_id`를 한 번의 쿼리로 조회해 걸러냅니다.


---

## 5. 구현

### 5.1 commerce-service

outbox는 전달 보장을 위한 기술 장치라 **전체가 `infrastructure` 안**에 있습니다.


```
product/infrastructure/
├── outbox/
│   ├── ProductOutbox, ProductOutboxPayload      엔티티 / payload
│   ├── ProductOutboxStatus, ProductOutboxEvent  상태 / 이벤트 종류
│   ├── ProductOutboxRepository                  SENT 제외 배치 조회
│   ├── ProductOutboxAppender                    데이터 적재
│   └── OutboxPollingScheduler                   폴링 + 상태 전이
└── client/
    ├── RecommendationHttpClient                 전송 (POST)
    ├── ProductClientConfig                      RestClient Bean 추가
    └── dto/ProductEventApiRequest               DTO
```

전송 어댑터는 outbox 밖, 다른 외부 호출들과 같은 자리에 둡니다.
이 서비스가 나가는 모든 호출(member / offer / recommendation)을 한곳에서 볼 수 있게 하기 위함입니다.

**

`createProduct`와 `updateProduct`는 모두 `PRODUCT_UPDATED`로 적재합니다.
수신 측이 upsert로 처리하므로 생성/수정을 구분할 필요가 없습니다. (#218 임베딩 시 upsert로 처리 예정)

**`product_outbox`**

```
id             bigint        PK, identity
inserted_at    timestamp     not null   이벤트 발생 시각 
updated_at     timestamp     not null
aggregate_type varchar(255)  not null   'Product'
aggregate_id   varchar(255)  not null   productId
event_type     varchar(255)  not null   PRODUCT_UPDATED | PRODUCT_DELETED
payload        jsonb         not null
status         varchar(255)  not null   PENDING | SENT | FAILED
retry_count    integer       not null   누적 실패 횟수 (제한 없음)
last_error     text                     마지막 실패 사유 (길이 제한 없음)
sent_at        timestamp

```


**상태 전이**

```
          ┌──── 전달 성공 ────▶ SENT (종착, 더 이상 집히지 않음)
          │
PENDING ──┤
   │      └──── 전달 실패 ────▶ FAILED
   │                              │  retry_count++
   └──────────────────────────────┘
        FAILED도 스케줄러가 다시 집어감 (재시도 횟수 제한 없음)
```

스케줄러는 `status <> SENT`로 조회합니다. **전달이 끝난 것만 제외**되므로, 상태가 늘어나도 조회 조건을 고칠 필요가 없습니다.
`FAILED`는 다음 주기에 다시 시도됩니다.
`retry_count`와 `last_error`는 조회 용도입니다.
`last_error`를 `text`로 둔 이유는 예외 메시지를 자르지 않고 그대로 남기기 위함입니다.


> `inserted_at`은 `@PrePersist` 콜백에서 찍히므로 **`persist()` 호출 시점**입니다.
> 커밋 시점이 아니라 이벤트가 실제로 발생한 시점입니다.

### 5.2 recommendation-service

inbox도 outbox와 같은 이유로 **전체가 `infrastructure` 안**에 있습니다.
commerce의 `infrastructure/outbox/`와 대칭입니다.

```
shop/dear/recommendation/
├── infrastructure/inbox/
│   ├── RecommendationInbox                  엔티티
│   ├── InboxStatus, InboxEventType          상태 / 이벤트 종류
│   ├── RecommendationInboxJpaRepository     JPA
│   └── InboxService                         데이터 적재
└── presentation/
    ├── InternalProductEventController       수신 엔드포인트
    └── dto/request/ProductEventRelayRequest 요청 DTO
```

**`recommendation_inbox`** (스키마: `recommendation`)

```
id             bigint        PK, identity
inserted_at    timestamp     not null
updated_at     timestamp     not null
event_id       bigint        not null   ← 발신 측 outbox id (멱등키)
aggregate_type varchar(255)  not null
aggregate_id   varchar(255)  not null
event_type     varchar(50)   not null
payload        jsonb         not null
status         varchar(30)   not null   PENDING | PROCESSED | FAILED
occurred_at    timestamp     not null   ← 발신 측 inserted_at

UNIQUE uk_recommendation_inbox_event_id (event_id)
INDEX  idx_recommendation_inbox_status_occurred_at     (status, occurred_at)
INDEX  idx_recommendation_inbox_aggregate_occurred_at  (aggregate_id, occurred_at)
```

엔티티가 제공하는 동작: `markAsProcessed()`, `markAsFailed()`, `isPending()`,
`isStaleAgainst(lastProcessedOccurredAt)`.

### 5.3 내부 API 

`POST /internal/recommendation/product-events` (recommendation-service, 포트 8083)

```json
[
  {
    "id": 1,
    "aggregateType": "Product",
    "aggregateId": "101",
    "eventType": "PRODUCT_UPDATED",
    "payload": { "productId": 101, "story": "가죽이 길들여진 스니커즈" },
    "occurredAt": "2026-08-25T14:13:54.169600"
  }
]
```

- `id`는 수신 측 멱등키입니다.
- `payload`는 문자열이 아닌 **객체**로 전달됩니다(json).
### 5.4 설정
현재는 5초마다 100건씩 송신하게 설정되어있습니다.
ai로직 모두 구현 후 테스트 하여 튜닝예정입니다.

| 키 | 기본값 | 위치 |
|---|---|---|
| `product.outbox.polling-interval-ms` | `5000` | commerce |
| `product.outbox.batch-size` | `100` | commerce |

---

## 6. 추후 고려사항 
현재는 outbox / inbox 테이블에 이벤트가 적재되고 삭제되지 않습니다.
inbox에서 eventId로 중복을 걸러내고 있어 데이터를 삭제처리 하지 않게 구현하였습니다. 
만약 누적량이 많아진다면 주기적으로 outbox / inbox 테이블 데이터를 삭제하는 배치처리 구현이 필요하빈다.

---

## 7. 운영 DDL  — 배포 전 필수

```sql
-- recommendation-service: 스키마 생성
CREATE SCHEMA IF NOT EXISTS recommendation;
-- inbox
CREATE TABLE IF NOT EXISTS recommendation.recommendation_inbox (
    id             bigint  NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    inserted_at    timestamp(6) without time zone NOT NULL,
    updated_at     timestamp(6) without time zone NOT NULL,
    event_id       bigint                       NOT NULL,
    aggregate_type character varying(255)       NOT NULL,
    aggregate_id   character varying(255)       NOT NULL,
    event_type     character varying(50)        NOT NULL,
    payload        jsonb                        NOT NULL,
    status         character varying(30)        NOT NULL,
    retry_count    integer                      NOT NULL,
    last_error     text,
    occurred_at    timestamp(6) without time zone NOT NULL,

    CONSTRAINT recommendation_inbox_pkey PRIMARY KEY (id),
    CONSTRAINT uk_recommendation_inbox_event_id UNIQUE (event_id),
    CONSTRAINT recommendation_inbox_event_type_check
    CHECK (event_type IN ('PRODUCT_UPDATED', 'PRODUCT_DELETED')),
    CONSTRAINT recommendation_inbox_status_check
    CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
    );

CREATE INDEX IF NOT EXISTS idx_recommendation_inbox_status_occurred_at
    ON recommendation.recommendation_inbox (status, occurred_at);

CREATE INDEX IF NOT EXISTS idx_recommendation_inbox_aggregate_occurred_at
    ON recommendation.recommendation_inbox (aggregate_id, occurred_at);

-- outbox
CREATE TABLE IF NOT EXISTS product_outbox (
    id             bigint NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    inserted_at    timestamp(6) without time zone NOT NULL,
    updated_at     timestamp(6) without time zone NOT NULL,
    aggregate_type character varying(255)       NOT NULL,
    aggregate_id   character varying(255)       NOT NULL,
    event_type     character varying(255)       NOT NULL,
    payload        jsonb                        NOT NULL,
    status         character varying(255)       NOT NULL,
    retry_count    integer                      NOT NULL,
    last_error     text,
    sent_at        timestamp(6) without time zone,

    CONSTRAINT product_outbox_pkey PRIMARY KEY (id),
    CONSTRAINT product_outbox_event_type_check
    CHECK (event_type IN ('PRODUCT_UPDATED', 'PRODUCT_DELETED')),
    CONSTRAINT product_outbox_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
    );

```



---
