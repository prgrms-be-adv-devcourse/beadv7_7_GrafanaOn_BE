# Recommendation Service - ai

관심상품의 Story를 기반으로 유사 상품을 추천하는 로직을 구현합니다.

상품 Story를 임베딩하여 `pgvector`에 저장하고, 저장된 벡터 간 **Cosine Distance**를 계산하여 유사한 상품의 ID를 응답값으로 반환합니다.

| 항목        | 내용                                                                     |
|-----------|------------------------------------------------------------------------|
| 추천 방식     | 콘텐츠 기반 필터링(CBF) - Cosine Similarity                                                     |
| Vector DB | PostgreSQL + pgvector                                                  |
| Embedding | Local `bge-m3` (Ollama) / Production `text-embedding-3-small` (OpenAI) |
| Dimension | Local 1024 (bge-m3) / Production 1536 (text-embedding-3-small)         |

---

## 목차

**설계 결정**

1. [Architecture](#1-architecture)
2. [추천 방식](#2-추천-방식)
3. [Vector DB](#3-vector-db)
4. [Embedding Model](#4-embedding-model)
5. [Embedding Dimension](#5-embedding-dimension)
6. [Vector Search 로직](#6-vector-search-로직)

**구성 / 운영**

7. [Event Pipeline](#7-event-pipeline)
8. [임베딩](#8-임베딩)
9. [추후 고려사항](#9-추후-고려사항)

---

## 1. Architecture

### 1.1 파이프라인

```text
commerce-service
      │
      │ 상품 등록 / 수정 / 삭제
      ▼
   Outbox
      │
      │ Background Relay
      ▼
 recommendation-service
      │
      ├─ Inbox
      │      ↓ Background Relay
      ├─ EmbeddingModel
      │      ├─ Local : BGE-M3 + Ollama
      │      └─ Prod  : OpenAI Embedding
      ▼
   pgvector
      │
      │ Cosine Distance
      ▼
유사 상품 조회
      │
      ▼
Recommendation API
```

1. 데이터 적재 파이프라인 (commerce → recommendation)
2. 임베딩 파이프라인 (inbox → pgvector)
3. 유사도 기반 응답 생성 (Cosine Distance)

### 1.2 구현 현황

| 단계               | 상태                          |
| ---------------- |-----------------------------|
| ① Outbox / Relay | 구현 중 — [rec_events.md](상품이벤트문서.md)        |
| ② 임베딩 파이프라인      | **구현 중** — [rec_embedding.md](AI임베딩문서.md) |
| ③ 유사 상품 조회 API   | 구현 중 — rec_similar.md       |

---

## 2. 추천 방식

> **콘텐츠 기반 필터링(CBF) - 의미적 유사도(Dense Retrieval)**

### 2.1 추천 알고리즘
콘텐츠 기반 필터링(CBF): 사용자와 아이템 간의 유사도를 계산해 가장 유사도가 높은 아이템 추천

협업 필터링(CF): 사용자와 비슷한 선호도를 가진 다른 사용자가 좋아하는 아이템 추천

회의를 통해 플랫폼 취지에 맞게 사용자가 선호하는 상품의 Story(판매자가 상품을 구입하고 소유하면서 갖게 된 이야기)를 기반으로 상품을 추천하는 것으로 논의하였습니다.

### 2.2 구현 방법

A 키워드 기반 추천(Sparse Retrieval), B 의미적 유사도(Dense Retrieval)

1) 아이템을 어떻게 표현할 것인가
- A 키워드를 추출하여 키워드 배열로 표현한다.
- B 벡터로 표현한다.

2) 추천 방법
- A 키워드가 겹치는 상품을 뽑아낸다
- B 벡터 공간 거리를 계산하여 유사한 상품을 뽑아낸다

단어가 정확히 일치하지 않더라도 문맥상 의미가 유사한 문서들을 검색할 수 있어, 복잡하거나 다양한 표현을 포함한 질문들에 대해 좀 더 높은 정확도를 보여주는 B안을 채택하였습니다.

유저의 관심 상품과 Cosine Distance를 계산하여 추천 상품을 뽑아냅니다.
(pgvector의 `<=>` 연산자)

```text
distance = 1 - similarity
similarity = 0.72  →  distance = 0.28
```

따라서 SQL에서는 distance < :maxDistance 처럼 **작을수록 유사한 상품**입니다.



---

## 3. Vector DB

> **PostgreSQL + pgvector**

후보: `SimpleVectorStore` / `PostgreSQL + pgvector` / `Elasticsearch dense_vector`


### 3.1 SimpleVectorStore

* 저장된 벡터를 DB에서 다시 주입하는 구조를 만들기 어렵습니다.
* 인메모리 Map 기반이라 기본적으로 단일 인스턴스 구조입니다.
* Scale-out 시 인스턴스마다 벡터 데이터가 달라질 수 있습니다.

컨테이너 기반 배포 및 Scale-out을 고려하면 적합하지 않다고 판단했습니다.

### 3.2 Elasticsearch dense_vector

기존 `commerce-service`에서 상품 검색을 위해 Elasticsearch를 사용하고 있습니다.

`SearchProductDocument.story_content` 역시 Nori Analyzer로 이미 색인되고 있어, `more_like_this`를 이용한 키워드 기반 유사 상품 추천도 가능합니다.

하지만 추천용 Vector Index까지 기존 검색 인덱스에 결합하면 검색 기능과 추천 기능의 결합도가 높아집니다.

기존 상품 검색 기능에 영향을 주지 않고 추천 기능을 독립적으로 운영하기 위해 적합하지 않다고 판단했습니다.

### 3.3 pgvector

* PostgreSQL이 벡터의 영속 저장소 역할을 수행
* 재시작 / 재배포와 무관하게 데이터 유지
* 별도의 벡터 저장소 운영 비용 감소
* SQL을 이용한 직접적인 벡터 비교 가능
* 조회 시 기준 상품을 다시 임베딩할 필요 없음
* Scale-out 가능

특히 **기준 상품의 기존 벡터를 그대로 비교에 사용할 수 있다는 점**을 핵심 장점으로 판단했습니다.

---

## 4. Embedding Model

> **로컬 : BGE-M3 / 운영 : text-embedding-3-small**

### 4.1 로컬

BGE-M3 VS nomic-embed-text

nomic-embed-text보다 BGE-M3가 한국어 의미적 변별력에서 우수하여 BGE-M3를 채택하였습니다.

### 4.2 운영

OpenAI text-embedding-3-small VS text-embedding-3-large

text-embedding-3-large는 text-embedding-3-small 대비 임베딩 품질과 검색 정확도가 높지만, 높은 차원의 벡터로 인해 저장 공간과 비용이 증가합니다. 서비스 요구사항을 충족하면서 비용 효율성이 높은 text-embedding-3-small을 채택하였습니다.

---

## 5. Embedding Dimension
> 로컬과 운영의 차원을 통일하지 않고 각 모델의 네이티브 차원을 그대로 씁니다.

Local과 Production에서 사용하는 모델의 차원이 다릅니다.

```text
BGE-M3                            1024 dimensions
OpenAI text-embedding-3-small     1536 dimensions
```


차원을 통일하는 방법도 있지만, 그렇게 해서 얻는 것은 **Schema 동일성 하나뿐**이므로 로컬 / 운영 별도로 진행합니다.

### 5.1 환경별 Schema 관리

```properties
# Local
recommendation.vector.dimensions=1024
recommendation.vector.initialize-schema=true

# Production
recommendation.vector.dimensions=1536
recommendation.vector.initialize-schema=false
```

Local은 기동 시 `ProductVectorSchemaInitializer`가 `vector(1024)` 테이블을 만들고, Production은 배포 전 Migration으로 `vector(1536)` 테이블을 만듭니다. ([6.3 Schema](#63-schema))

**감수하는 것** — 로컬에서 운영과 동일한 컬럼 타입을 재현하지 못합니다. 1536 기준의 Index Build 시간, 메모리 사용량, 저장 용량은 로컬 측정값으로 추정할 수 없으므로, 해당 수치는 운영 또는 동일 차원의 별도 환경에서 측정합니다.

---

## 6. Vector Search 로직

> **Raw SQL**

### 6.1 Spring AI Vector Store
Spring AI에서 제공하는 API를 사용하는 방식입니다.

자체적으로 vector_store 테이블을 생성해줍니다.

임베딩할 데이터를 Document로 생성만 하면 제공하는 API를 호출하여 저장 및 검색이 가능합니다.


```java
//예시코드
PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
    .distanceType(PgDistanceType.COSINE_DISTANCE)
    .indexType(PgIndexType.HNSW)
    .build();

// 저장 (임베딩 생성까지 자동으로 처리됨)
vectorStore.add(List.of(
    new Document(story, Map.of("productId", 123, "sellerId", 45))
));

// 검색
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(userProfileText)
        .withTopK(50)
        .withSimilarityThreshold(0.7)
        .withFilterExpression("sellerId != 45")
);
```


### 6.2 Raw SQL

테이블을 자체적으로 생성하고, 쿼리를 통해 통제합니다.

자유롭게 쿼리를 짤 수 있지만, 테이블 생성, 저장, 조회 등 쿼리를 직접 만들어야 하고 쿼리가 노출됩니다.

Hibernate 에는 `vector` 타입이 없어 `JdbcTemplate` 으로 직접 다뤄야 합니다.


```java
//예시코드

private final JdbcTemplate jdbcTemplate;

public List<ProductScore> findSimilarProducts(float[] userVector, int topK) {
	String sql = """
		SELECT id, title, 1 - (story_vector <=> ?::vector) AS similarity
		FROM product
		WHERE status = 'ACTIVE'
		ORDER BY story_vector <=> ?::vector
		LIMIT ?
		""";
	return jdbcTemplate.query(sql, ..., userVector, userVector, topK);
}
```

결론적으로는 벡터 검색 로직을 Spring AI 추상화에 맡길 것인가, 우리 도메인(product/scrap) 스키마와 비즈니스 로직에 맞춰 직접 통제할 것인가에 대한 고민입니다. 프레임워크의 편의성에 의존하는 것보다는 직접 플랫폼에 맞는 별도 테이블을 생성하여 통제하는 것이 유리할 것 같아 Raw SQL 방식을 채택하였습니다.

### 6.3 Schema

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS product_vector (
    product_id  BIGINT       PRIMARY KEY,
    story       TEXT         NOT NULL,   -- 임베딩에 넣은 원문. 재임베딩 판단 기준이다.
    model_name  VARCHAR(100) NOT NULL,   -- 모델이 다르면 벡터 공간도 다르다.
    embedding   VECTOR(1024) NOT NULL,   -- Local 1024 / Production 1536
    embedded_at TIMESTAMP    NOT NULL
);
```
벡터 바인딩은 `com.pgvector:pgvector` 의 `PGvector` 를 씁니다. `PGobject` 구현체라 드라이버가 그대로 바인딩하므로 문자열 조립이나 `::vector` 캐스팅이 필요 없습니다.

---

## 7. Event Pipeline
임베딩에 필요한 데이터를 적재할 파이프라인을 구현합니다.

**Outbox Pattern + Background Relay**를 사용합니다.

### 7.1 구조

```text
상품 등록 / 수정
      │
      ├── 상품 저장
      │
      └── Outbox INSERT
             │
             │ Transaction Commit
             ▼
          Outbox
             │
             │ Background Relay
             ▼
        Recommendation
             │
             ▼
           Inbox
```

상품 저장과 Outbox INSERT는 **동일 트랜잭션**에서 처리합니다. 현재 `SpringProductEventPublisher` 는 커밋 이후 In-process 로 발행하기 때문에, 프로세스가 발행 전에 죽으면 이벤트가 유실됩니다.

### 7.2 Inbox 멱등성

`recommendation_inbox` 에 `event_id UNIQUE` 제약을 적용합니다. Relay가 동일 이벤트를 여러 번 전달하더라도 중복 적재되지 않습니다.

### 7.3 중복 방지
Outbox의 이벤트가 재처리 등의 이유로 1번 이상 전달될 경우, 중복을 방지하기 위해 사전에 이미 처리된 이벤트인지를 검증합니다.


---

## 8. 임베딩

### 8.1 순서 역전

이벤트의 **발생 순서와 도착 순서가 달라질 수 있습니다.** 늦게 도착한 과거 Story 가 최신 Story 를 덮어쓰지 않도록, 같은 상품에서 이미 처리한 이벤트의 `occurred_at` 과 비교해서 걸러냅니다.

### 8.2 Re-embedding 최적화

Story가 변경되지 않았다면 다시 임베딩하지 않습니다.

`product_vector.story` 에는 임베딩에 넣은 텍스트가 그대로 들어 있습니다. 즉 이 컬럼이 곧 "이 벡터를 만든 원문" 이므로, 새 Story 와 비교하면 재임베딩 여부를 바로 판단할 수 있습니다.

```sql
SELECT story FROM product_vector
 WHERE product_id = :productId AND model_name = :modelName
```

이 값이 새 Story 와 같으면 건너뛰고, 다르거나 없으면 임베딩합니다.

### 8.3 부가정보만 변경되는 경우

`SOLD_OUT` 상태 변경처럼 Story는 그대로이고 부가정보만 바뀌는 경우, Vector를 재생성할 필요가 없습니다.

현재 `product_vector` 에는 메타데이터 컬럼이 없으므로 해당 요구가 생길 때 컬럼과 갱신 경로를 추가합니다.


### 8.4 처리 방식

```text
상품 이벤트
      │
      ▼
저장된 story 조회 (모델명 일치 조건 포함)
      │
      ├─ 동일 → Skip
      │
      └─ 다름 / 없음 → Embedding
                        │
                        ▼
                    pgvector
```

---

## 9. 추후 고려사항

### 9.1 Vector Index

상품 수가 적다면 Sequential Scan이 오히려 빠를 수 있으므로 실제 데이터를 측정한 뒤 도입을 검토합니다.

| 방식         | 장점                     | 단점                                     |
| ---------- | ---------------------- | -------------------------------------- |
| HNSW       | 조회 성능이 빠름 / 데이터 추가에 강함 | Build 비용 높음 / 메모리 사용량 큼 / Recall 손실 가능 |
| IVFFlat    | Build가 상대적으로 빠름        | 데이터 증가 시 재빌드 필요                        |
| Sequential | Index 불필요 / 정확도 손실 없음  | 데이터 증가 시 선형적으로 느려짐                     |

초기에는 Index 없이 Raw SQL로 조회합니다.

```text
상품 수 → EXPLAIN ANALYZE → 조회 latency → Index 필요성 판단
```

### 9.2 Similarity Threshold

일정 수준보다 유사하지 않은 결과를 제외하는 기준값입니다.

실제 상품 Story 데이터를 수집한 뒤 유사도 분포를 확인하여 결정합니다.
