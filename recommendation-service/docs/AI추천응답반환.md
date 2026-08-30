# 유사 상품 조회

| 항목        | 현재 상태                                          |
| --------- | ---------------------------------------------- |
| 기준        | `productId`                  |
| 거리        | Cosine Distance (`<=>`). 작을수록 유사               |
| Threshold | `0.45` — 시드 147건으로 측정한 잠정값  |

---

## 1. 전체 흐름

```text
GET /api/recommendations/similar?productId=101&size=10 (임시)
        │
        ▼
RecommendationController
        │  
        ▼
RecommendationService           
        │
        ├─ 1. productId 검증
        │
        └─ 2. 조회 (maxDistance, limit)
                 │
                 ▼
        ProductVectorRepository (포트)
                 │
        ProductVectorJdbcAdapter        
                 │  self join 1회. 계산은 전부 DB 안에서 끝난다
                 ▼
              pgvector
                 │
                 ▼
        List<RecommendationItem>   (productId, distance) — 가까운 순
                 │
                 ▼
RecommendSimilarItemsResponse.listOf()
                 │  
                 ▼
        [{productId, rank}, ...]
```


---

## 2. 조회 쿼리

```sql
SELECT s.product_id                AS product_id,
       s.embedding <=> b.embedding AS distance
  FROM recommendation.product_vector s,      -- s = 후보
       recommendation.product_vector b       -- b = 기준
 WHERE b.product_id  = ?
   AND s.product_id <> b.product_id
   AND (s.embedding <=> b.embedding) < ?
 ORDER BY distance
 LIMIT ?
```

`JdbcTemplate` 은 Hibernate 의 `default_schema` 를 따르지 않고 커넥션의 `search_path` 를 그대로 타기 때문에 recommendation을 붙여 명시해줍니다.

<=> Cosine Distance를 계산합니다. 1 - 유사도 값으로, 값이 작을수록 유사한 상품입니다.

index를 넣지 않았습니다. HNSW index를 얹게 되면 검색 속도와 정확도를 얻는 대신, 메모리와 인덱싱 비용을 희생하는 트레이드 오프가 발생합니다. 현재 저희 프로젝트의 트래픽에서는 트레이드 오프로 잃는 점이 더 많다고 판단하여, 지금 규모에서는 적용하지 않았습니다.

## 3. 설정

```yaml
recommendation:
  similar:
    max-distance: ${RECOMMENDATION_MAX_DISTANCE:0.45}
    default-size: ${RECOMMENDATION_DEFAULT_SIZE:10}
    max-size:     ${RECOMMENDATION_MAX_SIZE:50}
```
max-distance (최대 허용 거리) = Threshold (임계값)

무분별하게 연관성이 낮은 상품이 추천되는 것을 막기 위한 임계값을 설정했습니다.

운영 더미데이터를 포함한 147건으로 실측하였습니다.

---

## 4. 검증

### 4.1 준비

```bash
./gradlew :recommendation-service:bootRun
```

### 4.2 실호출

벡터 147건(`bge-m3`) 기준입니다.

```bash
curl "localhost:8084/api/recommendations/similar?productId=101&size=5"
```

```json
[{"productId":103,"rank":1},{"productId":102,"rank":2},{"productId":104,"rank":3},
 {"productId":2000034,"rank":4},{"productId":201,"rank":5}]
```


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

