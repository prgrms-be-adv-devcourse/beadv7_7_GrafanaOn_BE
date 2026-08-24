# Recommendation Service - ai

관심상품의 Story를 기반으로 유사 상품을 추천하는 로직을 구현합니다.

상품 Story를 임베딩하여 `pgvector`에 저장하고, 저장된 벡터 간 **Cosine Distance**를 계산하여 유사한 상품의 ID를 응답값으로 반환합니다.

| 항목        | 내용                                                                     |
|-----------|------------------------------------------------------------------------|
| 추천 방식     | 벡터 임베딩 기반 유사도                                                          |
| Vector DB | PostgreSQL + pgvector                                                  |
| Embedding | Local `bge-m3` (Ollama) / Production `text-embedding-3-small` (OpenAI) |
| Dimension | Local 1024 (bge-m3) / Production 1536 (text-embedding-3-small)         |

---

## 목차

**설계 결정**

1. [Architecture](#1-architecture)
2. [추천 방식 — Keyword vs Embedding](#2-추천-방식--keyword-vs-embedding)
3. [Vector DB 선택](#3-vector-db-선택)
4. [Embedding Model 선택](#4-embedding-model-선택)
5. [Embedding Dimension](#5-embedding-dimension)
6. [Similarity Threshold](#6-similarity-threshold)
7. [Vector Search 방식](#7-vector-search-방식)

**구성 / 운영**

8. [Schema](#8-schema)
9. [Production Schema 관리](#9-production-schema-관리)
10. [Event Pipeline](#10-event-pipeline)
11. [Re-embedding 최적화](#11-re-embedding-최적화)
12. [Vector Index](#12-vector-index)

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
3. 유사도 기반 응답 생성 (Cosine Distance + ChatModel)

### 1.2 구현 현황

| 단계               | 상태                                        |
| ---------------- |-------------------------------------------|
| ① Outbox / Relay | 구현 전                                      |
| ② 임베딩 파이프라인      | **구현 중** — [embedding.md](./embedding.md) |
| ③ 유사 상품 조회 API   | 구현 전                                      |

---

## 2. 추천 방식 — Keyword vs Embedding

> 키워드 기반 추천 대신 **Embedding 기반 추천**을 사용합니다.

| 방식                 | Spring AI        | 용도           |
| ------------------ | ---------------- | ------------ |
| Embedding          | `EmbeddingModel` | 의미 기반 유사도 계산 |
| Keyword Extraction | `ChatModel`      | 텍스트에서 키워드 추출 |

### 2.1 Keyword 추출

더미데이터 : 2019년 파리 여행에서 산 나이키 에어조던1 시카고입니다. 첫 출근날 신고 갔던 신발이라 애착이 많아요.

프롬프트 : 다음 상품 설명에서 검색과 유사도 판단에 쓸 핵심 키워드를 5개 이하로 뽑아라.

```bash
docker compose -f ../common/docker/ollama/docker-compose.local.yaml up -d
docker exec ollama-local ollama pull llama3.1

curl -s http://localhost:11434/api/generate \
  -H 'Content-Type: application/json' \
  -d @bench/req.json | node bench/show.js
```

결과:

```text
{ "keywords": [ "나이키 에어조던1", "시카고", "파리", "에어조던", "나이키" ] }
```

문제점:

* `나이키 에어조던1` / `에어조던` / `나이키` — 5개 중 3개가 같은 신발의 변형
* `파리`, `시카고`처럼 상품 유사도 판단에 불필요한 키워드 포함
* 키워드 추출 결과의 품질에 따라 추천 결과가 좌우됨

### 2.2 Embedding

Story 전체를 벡터 하나로 만들어 벡터 공간에서 비교합니다.

### 2.3 실측 비교

같은 Story 10건, 로컬 CPU 기준입니다.

|            | 콜드                     | 웜         |
| ---------- | ---------------------- | --------- |
| 키워드 추출 (llama3.1) | 123초 (모델 로딩 80초 포함)     | **22.9초** |
| 임베딩 (bge-m3)      | 10.8초 (로딩)              | **0.45초** |

임베딩은 forward pass 한 번이지만, 챗 모델은 **토큰을 하나씩 생성**하여 생성 시간이 출력 길이에 그대로 비례합니다.

---

## 3. Vector DB 선택

후보: `SimpleVectorStore` / `PostgreSQL + pgvector` / `Elasticsearch dense_vector`

> **PostgreSQL + pgvector**

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

특히 **기준 상품의 기존 벡터를 그대로 비교에 사용할 수 있다는 점**을 핵심 장점으로 판단했습니다. ([7. Vector Search 방식](#7-vector-search-방식))

---

## 4. Embedding Model 선택

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


양쪽으로 맞추는 방법이 있지만 차원을 맞춰 얻는 것은 **Schema 동일성 하나뿐**이므로 로컬 / 운영 별도로 진행합니다.

### 5.1 환경별 Schema 관리

```properties
# Local
recommendation.vector.dimensions=1024
recommendation.vector.initialize-schema=true

# Production
recommendation.vector.dimensions=1536
recommendation.vector.initialize-schema=false
```

Local은 기동 시 `ProductVectorSchemaInitializer`가 `vector(1024)` 테이블을 만들고, Production은 배포 전 Migration으로 `vector(1536)` 테이블을 만듭니다. ([9. Production Schema 관리](#9-production-schema-관리))

**감수하는 것** — 로컬에서 운영과 동일한 컬럼 타입을 재현하지 못합니다. 1536 기준의 Index Build 시간, 메모리 사용량, 저장 용량은 로컬 측정값으로 추정할 수 없으므로 ([12.1](#121-production1536차원에서의-제약)), 해당 수치는 운영 또는 동일 차원의 별도 환경에서 측정합니다.

---

## 6. Similarity Threshold

일정 수준보다 유사하지 않은 결과를 제외하는 기준값입니다.

실제 상품 Story 데이터를 수집한 뒤 유사도 분포를 확인하여 결정합니다.

### 6.1 Cosine Similarity와 Distance

pgvector의 `<=>` 연산자는 Similarity가 아니라 **Cosine Distance**를 반환합니다.

```text
distance = 1 - similarity

similarity = 0.72  →  distance = 0.28
```

따라서 SQL에서는

```sql
distance < :maxDistance
```

처럼 **작을수록 유사한 상품**입니다. 

### 6.2 검증 계획

실제 Story 20~30건을 대상으로:

1. 응답 후보 선정
2. 유사도 계산
3. 거리 분포 확인
4. 적절한 Threshold 결정
5. 품질 검증

---

## 7. Vector Search 방식

> 기준 상품의 Vector를 **DB에서 직접 가져와 Raw SQL로 비교합니다.**

Spring AI `VectorStore` API 는 텍스트를 입력으로 받기 때문에, 조회할 때마다 기준 상품을 다시 임베딩해야 합니다. 이미 벡터가 DB 에 있는 상황에서는 불필요한 비용입니다.

```sql
SELECT
    s.product_id,
    s.story,
    s.embedding <=> b.embedding AS distance
FROM product_vector s,
     product_vector b
WHERE b.product_id = :productId
  AND s.product_id <> b.product_id
  AND s.embedding <=> b.embedding < :maxDistance
ORDER BY distance
LIMIT :k;
```

조회 흐름:

```text
productId
   │
   ▼
product_vector의 기준 Vector 조회
   │
   ▼
DB 내부에서 Vector ↔ Vector 비교
   │
   ▼
Cosine Distance 정렬
   │
   ▼
Top-K 반환
```

추후 INDEX 도입을 검토합니다.

```text
Vector Search
    │
    ├── HNSW → 검색 속도 개선
    │
    ├── Cosine Distance → 유사도 측정
    │
    ├── Threshold → 너무 안 비슷한 결과 제거
    │
    └── Top-K → 최종 몇 개 가져올지 결정
```

---

## 8. Schema
> 프레임워크 스키마를 쓰지 않고 **테이블을 직접 정의합니다.**

Spring AI 의 `PgVectorStore` 를 쓰면 `vector_store (id text, content text, metadata json, embedding vector)` 스키마를 그대로 써야 합니다. 컬럼 모양이 프레임워크에 고정되어 `productId` 가 `text` 가 되고, 모델명 같은 정보는 `metadata` json 안에 묻힙니다.


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

### 8.1 JPA Entity 로 관리하지 않는 이유

Hibernate 에는 `vector` 타입이 없습니다. `ddl-auto` 가 이 테이블을 만들어주지 못하고, `float[]` 를 매핑할 방법도 없습니다.

따라서 `product_vector` 는 `JdbcTemplate` 으로 직접 다룹니다 .

벡터 바인딩은 `com.pgvector:pgvector` 의 `PGvector` 를 씁니다. `PGobject` 구현체라 드라이버가 그대로 바인딩하므로 문자열 조립이나 `::vector` 캐스팅이 필요 없습니다.

---

## 9. Production Schema 관리

운영에서는 애플리케이션이 Schema를 만들지 않습니다.

```properties
recommendation.vector.initialize-schema=false
```

Extension과 Table은 **배포 전에 DB Migration으로 생성**합니다. (`sql/schema-product-vector.sql`)

`CREATE EXTENSION` 은 superuser 권한이 필요한데 운영 DB 계정에 없을 수 있어, 애플리케이션 기동 시점에 만들지 않습니다.

---

## 10. Event Pipeline

상품 이벤트는 **Outbox Pattern + Background Relay**를 사용합니다.

### 10.1 구조

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

### 10.2 Inbox 멱등성

`recommendation_inbox` 에 `event_id UNIQUE` 제약을 적용합니다. Relay가 동일 이벤트를 여러 번 전달하더라도 중복 적재되지 않습니다.

### 10.3 순서 역전

Relay 가 재시도하면 **발생 순서와 도착 순서가 달라질 수 있습니다.** 늦게 도착한 과거 Story 가 최신 Story 를 덮어쓰지 않도록, 같은 상품에서 이미 처리한 이벤트의 `occurred_at` 과 비교해서 걸러냅니다.

---

## 11. Re-embedding 최적화

Story가 변경되지 않았다면 다시 임베딩하지 않습니다.

`product_vector.story` 에는 임베딩에 넣은 텍스트가 그대로 들어 있습니다. 즉 이 컬럼이 곧 "이 벡터를 만든 원문" 이므로, 새 Story 와 비교하면 재임베딩 여부를 바로 판단할 수 있습니다.

```sql
SELECT story FROM product_vector
 WHERE product_id = :productId AND model_name = :modelName
```

이 값이 새 Story 와 같으면 건너뛰고, 다르거나 없으면 임베딩합니다.

### 11.1 처리 방식

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

### 11.2  부가정보만 변경되는 경우

`SOLD_OUT` 상태 변경처럼 Story는 그대로이고 부가정보만 바뀌는 경우, Vector를 재생성할 필요가 없습니다.

현재 `product_vector` 에는 메타데이터 컬럼이 없으므로 해당 요구가 생길 때 컬럼과 갱신 경로를 추가합니다.

---

## 12. Vector Index

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


---
