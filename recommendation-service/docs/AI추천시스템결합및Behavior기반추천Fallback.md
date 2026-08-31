# AI 추천 시스템 결합 및 Behavior 기반 추천 Fallback

## 1. 개요

추천 서비스는 다음 두 추천 결과를 결합한다.

- **Behavior 추천**: 사용자 관심 카테고리와 상품 인기도를 기반으로 생성하는 기본 추천
- **AI 유사 상품 추천**: 상품 Story Embedding과 pgvector를 이용해 기준 상품과 유사한 상품을 탐색하는 추천

Behavior 추천은 API 응답의 기본 결과와 가용성을 책임진다. AI 추천은 Behavior 추천의 Top-1 상품을 기준으로 유사 상품을 탐색하여 추천 품질과 다양성을 확장한다.

AI 추천은 추천 API의 필수 경로가 아니다. 기준 상품의 Vector가 없거나 유사 상품 검색에 실패하더라도 전체 추천 API가 실패하지 않도록 Behavior 추천 결과로 Fallback한다.

> Behavior 추천은 기본 추천 결과와 가용성을 책임지고, AI 추천은 추천 품질과 다양성을 확장한다.

---

## 2. 설계 목표

### 2.1 Behavior 추천의 역할

Behavior 추천은 추천 API의 기본 결과를 생성한다.

- 사용자 행동 기반 관심 카테고리 반영
- 관심도가 높은 카테고리를 추천 후보 조회 조건으로 사용
- 판매 가능한 `ON_SALE` 상품만 후보로 선정
- 카테고리 관심도와 상품 인기도를 기반으로 점수 계산
- AI 추천 성공 여부와 관계없이 먼저 기본 추천 결과 확보

### 2.2 AI 추천의 역할

AI 추천은 Behavior 추천 결과를 기준으로 상품 Story가 유사한 상품을 탐색한다.

- 상품 Story Embedding 활용
- pgvector 기반 Cosine Distance 검색
- Behavior 추천 Top-1 상품을 기준 상품으로 사용
- 판매 가능한 AI 추천 결과만 사용
- 유효한 AI 결과를 최종 추천 결과의 앞부분에 배치

### 2.3 Fallback의 역할

AI 추천 시스템의 문제로 전체 추천 API가 실패하지 않도록 한다.

다음 상황에서는 Behavior 추천 결과를 반환한다.

- Behavior 기준 상품의 Vector가 존재하지 않는 경우
- pgvector 유사 상품 검색 결과가 없는 경우
- 검색된 AI 상품이 모두 판매 불가능한 경우
- AI 추천 처리 중 예외가 발생한 경우
- 유효한 AI 추천 결과를 생성하지 못한 경우

---

## 3. 전체 추천 흐름

```text
[추천 API 요청]

GET /api/recommendations

memberId = @AuthUser
limit = 요청 추천 개수

        ↓

[RecommendationFacadeService]

        ↓

1. 요청값 검증

1 <= limit <= MAX_RECOMMENDATION_LIMIT

        ↓

2. Behavior RecommendationContext 생성

UserInterest 조회

        ↓

관심도 상위 카테고리 추출

        ↓

해당 카테고리의 ON_SALE 상품 조회

        ↓

RecommendationContext
- interests
- candidates

        ↓

3. Behavior 추천 생성

categoryInterestScore
        +
log(1 + viewCount)

        ↓

Behavior Score 내림차순 정렬

        ↓

Top-N Behavior 추천

        ↓

4. AI 기준 상품 선정

Behavior 추천 Rank 1

        ↓

baseProductId

        ↓

5. AI 유사 상품 추천

baseProductId의 Vector 조회

        ↓

pgvector Cosine Distance 검색

        ↓

기준 상품을 제외한 유사 상품 Top-N

        ↓

6. AI 추천 결과 검증

RecommendationItem 조회

        ↓

ON_SALE 상품만 유지

        ↓

┌─────────────────────┴─────────────────────┐
│                                           │
│ AI 결과 존재                              │ AI 결과 없음 또는 실패
│                                           │
↓                                           ↓

AI 결과 우선 배치                           Behavior 결과 반환

        ↓

부족한 개수는 Behavior 결과로 보충

        ↓

productId 기준 중복 제거

        ↓

limit 적용

        ↓

rank 재계산

        ↓

[최종 추천 응답]

recommendationId
items[]
```

---

## 4. Behavior 추천

### 4.1 사용자 관심도 조회

`UserInterestRepository`를 통해 사용자의 카테고리별 관심도를 높은 순서로 조회한다.

```java
List<UserInterest> interests =
        userInterestRepository.findByMemberIdOrderByScoreDesc(memberId);
```

관심 데이터가 없으면 추천에 필요한 Context를 생성할 수 없으므로 빈 추천 결과를 반환한다.

```text
UserInterest 없음
        ↓
RecommendationContext.empty()
        ↓
BasicRecommendationResponse.empty()
```

현재 빈 추천 응답은 다음과 같은 특성을 가진다.

- `recommendationId`: `null`
- `items`: 빈 목록
- AI 추천 호출: 수행하지 않음

---

### 4.2 상위 관심 카테고리 선정

현재 최대 3개의 관심 카테고리를 추천 후보 생성에 사용한다.

```java
private static final int TOP_CATEGORY_LIMIT = 3;
```

예시는 다음과 같다.

```text
UserInterest

SNEAKERS       15
SPORTS_SHOES   10
BOOTS           7
SANDALS         2

        ↓

Top Categories

SNEAKERS
SPORTS_SHOES
BOOTS
```

---

### 4.3 Behavior Candidate 생성

선정된 관심 카테고리를 기반으로 [RecommendationItem](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/domain/model/RecommendationItem.java:11:0-64:1)에서 추천 후보를 조회한다.

이 단계에서는 사용자에게 추천 가능한 `ON_SALE` 상품만 Candidate에 포함한다.

```text
Top Categories
        ↓
RecommendationItem 조회
        ↓
ProductStatus.ON_SALE 필터
        ↓
Behavior Candidates
```

관심 데이터가 존재하더라도 조회된 Candidate가 없다면 빈 [RecommendationContext](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/application/dto/RecommendationContext.java:7:0-26:40)를 반환한다.

---

### 4.4 Behavior Score 계산

Behavior 추천 점수는 다음 두 요소로 구성한다.

```text
Behavior Score

=

Category Interest Score

+

Popularity Score
```

상품 인기도 점수는 다음 식으로 계산한다.

```java
Math.log1p(item.getViewCount())
```

최종 Behavior Score는 다음과 같다.

```text
score

=

categoryInterestScore

+

log(1 + viewCount)
```

`log1p`를 사용하는 이유는 조회수가 매우 높은 상품의 인기도 점수가 카테고리 관심도 점수를 지나치게 압도하는 것을 완화하기 위해서다.

---

### 4.5 Behavior Ranking

각 Candidate의 Behavior Score를 계산한 후 점수 내림차순으로 정렬한다.

```text
Behavior Candidates
        ↓
Behavior Score 계산
        ↓
score DESC
        ↓
limit 적용
        ↓
rank 부여
```

Behavior 추천은 요청한 `limit` 범위에서 가능한 만큼 반환한다.

Candidate 수가 `limit`보다 적으면 요청한 개수를 모두 보장하지는 않는다.

---

## 5. AI 유사 상품 추천

### 5.1 상품 Vector 생성 및 저장

AI 유사 상품 추천은 상품 Story를 Embedding하여 `product_vector` 테이블에 저장한 데이터를 사용한다.

```text
Product Story
        ↓
Embedding Model
        ↓
Embedding Vector
        ↓
product_vector
```

주요 데이터는 다음과 같다.

```text
product_id
story
model_name
embedding
embedded_at
```

Embedding 저장 및 조회는 `ProductVectorRepository`를 통해 처리한다.

---

### 5.2 AI 기준 상품 선정

현재 `UserInterest`는 상품 단위가 아닌 카테고리 단위 관심도를 저장한다.

따라서 사용자가 가장 선호하는 특정 상품을 `UserInterest`만으로 직접 결정할 수 없다.

현재 구현에서는 Behavior 추천 결과 중 가장 높은 점수를 받은 Top-1 상품을 AI 유사 상품 검색의 기준으로 사용한다.

```text
Behavior Result

Rank 1 → productId 100
Rank 2 → productId 200
Rank 3 → productId 300

        ↓

baseProductId = 100
```

이 방식은 별도의 상품 단위 선호도 모델을 추가하지 않고 기존 Behavior 추천과 AI 유사 상품 추천을 연결하기 위한 V1 정책이다.

Behavior 추천 결과가 비어 있으면 기준 상품을 선택할 수 없으므로 AI 추천을 호출하지 않는다.

---

### 5.3 pgvector 유사 상품 검색

기준 상품의 Vector와 다른 상품 Vector 사이의 Cosine Distance를 계산한다.

```sql
s.embedding <=> b.embedding
```

Cosine Distance가 작을수록 기준 상품과 유사한 상품으로 판단한다.

```sql
ORDER BY distance
```

설정된 최대 Distance보다 가까운 상품만 조회한다.

```sql
AND (s.embedding <=> b.embedding) < :maxDistance
```

기준 상품 자체는 다음 조건으로 검색 결과에서 제외한다.

```sql
AND s.product_id <> b.product_id
```

따라서 pgvector 검색 단계에서는 기준 상품이 자신의 유사 상품으로 반환되지 않는다.

최종 SQL의 주요 흐름은 다음과 같다.

```sql
SELECT
    s.product_id,
    s.embedding <=> b.embedding AS distance
FROM product_vector s,
     product_vector b
WHERE b.product_id = :baseProductId
  AND s.product_id <> b.product_id
  AND (s.embedding <=> b.embedding) < :maxDistance
ORDER BY distance
LIMIT :limit;
```

---

### 5.4 AI 추천 결과의 판매 상태 검증

pgvector 유사 상품 검색 결과를 곧바로 최종 추천에 사용하지 않는다.

[VectorAiRecommendationAdapter](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/infrastructure/ai/VectorAiRecommendationAdapter.java:19:0-77:1)는 검색된 유사 상품 ID를 [RecommendationItemRepository](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/domain/repository/RecommendationItemRepository.java:7:0-10:1)에서 조회하고 다음 조건을 만족하는 상품만 유지한다.

- [RecommendationItem](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/domain/model/RecommendationItem.java:11:0-64:1)에 존재하는 상품
- 상품 상태가 `ProductStatus.ON_SALE`인 상품

```text
pgvector 유사 상품
        ↓
RecommendationItem 조회
        ↓
RecommendationItem 존재 여부 확인
        ↓
ProductStatus.ON_SALE 확인
        ↓
유효한 AI 추천 결과
```

검증 과정에서 모든 AI 결과가 제거되면 AI 추천 결과 없음으로 처리하고 Behavior 추천으로 Fallback한다.

---

### 5.5 AI Score 변환

pgvector 조회 결과는 Cosine Distance를 반환한다.

현재 구현에서는 Distance를 다음과 같이 AI Score로 변환한다.

```text
AI Score

=

1.0 - Cosine Distance
```

코드 수준의 정책은 다음과 같다.

```java
double score = 1.0 - item.distance();
```

Distance가 작을수록 AI Score가 높아진다.

AI 추천 결과는 pgvector SQL의 `ORDER BY distance` 순서를 유지한다. 결합 단계에서 AI Score를 이용한 별도의 재정렬은 수행하지 않는다.

---

### 5.6 AI Adapter의 역할

[VectorAiRecommendationAdapter](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/infrastructure/ai/VectorAiRecommendationAdapter.java:19:0-77:1)는 Behavior 추천 계층의 [AiRecommendationPort](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/application/port/AiRecommendationPort.java:6:0-12:1)를 구현하고 기존 Vector 추천 시스템을 연결한다.

```text
RecommendationFacadeService
        ↓
AiRecommendationPort
        ↓
VectorAiRecommendationAdapter
        ↓
RecommendationService
        ↓
ProductVectorRepository
        ↓
pgvector
```

Adapter의 책임은 다음과 같다.

- 기준 상품 ID를 기존 [RecommendationService](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/application/RecommendationService.java:12:0-60:1)에 전달
- pgvector 유사 상품 결과 수신
- 유사 상품의 판매 상태 검증
- Cosine Distance를 AI Score로 변환
- 유효한 AI 결과가 없으면 [Optional.empty()](cci:1://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/application/dto/RecommendationContext.java:17:4-22:5) 반환
- 예외 발생 시 [Optional.empty()](cci:1://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/application/dto/RecommendationContext.java:17:4-22:5)로 변환하여 Fallback 지원

---

## 6. Behavior + AI 결합 정책

### 6.1 Behavior 결과 선확보

AI 추천을 호출하기 전에 Behavior 추천을 먼저 완료한다.

```text
Behavior 추천 생성
        ↓
기본 결과 확보
        ↓
Behavior Top-1 선정
        ↓
AI 추천 시도
```

AI 추천을 먼저 수행한 뒤 실패했을 때 Behavior 추천을 다시 계산하는 구조가 아니다.

이를 통해 AI 추천 시스템의 상태와 관계없이 반환 가능한 기본 결과를 먼저 확보한다.

---

### 6.2 AI 정상 처리

유효한 AI 유사 상품 결과가 존재하면 AI 결과를 최종 추천의 앞부분에 배치한다.

```text
Behavior

100
200
300
400
500

AI

801
802
803

        ↓

Final

801
802
803
100
200
```

AI 결과만으로 요청한 `limit`을 만족하지 못하면 Behavior 추천 결과로 부족한 개수를 보충한다.

---

### 6.3 중복 제거

AI 결과와 Behavior 결과에 동일한 상품이 존재할 수 있으므로 `productId`를 기준으로 중복을 제거한다.

현재 `LinkedHashMap`을 사용하여 삽입 순서를 유지한다.

```java
Map<Long, Double> scoresByProductId =
        new LinkedHashMap<>();
```

AI 결과를 먼저 삽입한 다음 Behavior 결과를 삽입한다.

```java
aiResult.items().forEach(item ->
        scoresByProductId.putIfAbsent(
                item.productId(),
                item.score()
        )
);
behaviorResult.items().forEach(item ->
        scoresByProductId.putIfAbsent(
                item.productId(),
                item.score()
        )
);
```

동일한 상품이 AI와 Behavior 결과에 모두 존재하면 먼저 삽입된 AI 결과의 위치와 Score가 유지된다.

```text
AI

200
801
802

Behavior

100
200
300
400
500

        ↓

중복 제거 후

200
801
802
100
300
```

---

### 6.4 최종 limit 및 rank 적용

중복 제거가 끝난 결과에 최종 `limit`을 적용한다.

그 후 최종 노출 순서를 기준으로 rank를 다시 계산한다.

```text
병합 결과
        ↓
중복 제거
        ↓
limit 적용
        ↓
rank = index + 1
```

최종 rank는 AI 내부 rank 또는 Behavior 내부 rank를 그대로 사용하지 않는다.

---

### 6.5 AI 결과가 없는 경우

AI 추천 결과가 없으면 Behavior 결과를 그대로 반환한다.

```text
Behavior

100
200
300
400
500

AI

[]

        ↓

Final

100
200
300
400
500
```

이 경우 Behavior 추천 생성 시 사용한 `recommendationId`를 그대로 유지한다.

---

### 6.6 AI 처리 실패

AI 추천 Adapter는 유사 상품 검색 과정에서 발생한 예외를 처리한다.

```text
pgvector 검색
        ↓
Exception
        ↓
VectorAiRecommendationAdapter
        ↓
Optional.empty()
        ↓
RecommendationFacadeService
        ↓
Behavior Result 반환
```

AI 기능의 장애가 전체 추천 API 실패로 전파되지 않도록 하는 것이 목적이다.

현재 Adapter는 AI 추천 경로에서 발생하는 `Exception`을 처리하여 Behavior Fallback이 가능하도록 한다.

---

### 6.7 AI 결과 조회 개수의 현재 정책

현재 pgvector에서는 요청받은 `limit`만큼 유사 상품을 조회한 다음 `ON_SALE` 검증을 수행한다.

```text
pgvector Top-N 조회
        ↓
ON_SALE 검증
        ↓
일부 상품 제거 가능
        ↓
부족한 개수는 Behavior 추천으로 보충
```

예를 들어 `limit=5`인 요청에서 유사 상품 5개 중 3개가 판매 불가능하다면 유효한 AI 결과는 2개가 된다.

```text
pgvector 결과

801 ON_SALE
802 SOLD_OUT
803 ON_SALE
804 STOPPED
805 SOLD_OUT

        ↓

유효한 AI 결과

801
803

        ↓

나머지 3개는 Behavior 추천으로 보충
```

현재 정책은 최종 결과의 가용성을 Behavior 추천으로 보장하는 방식이다.

향후 AI 추천 비중을 더 높일 필요가 있다면 pgvector에서 `limit`보다 많은 후보를 조회한 뒤 판매 상태를 검증하는 초과 조회 정책을 고려할 수 있다.

예:

```text
vectorCandidateLimit = limit * 2
```

단, 이 경우 기존 `recommendation.similar.max-size` 설정과 조회 비용을 함께 고려해야 한다.

---

## 7. Score 정책

Behavior Score와 AI Score는 동일한 의미의 값이 아니다.

### 7.1 Behavior Score

Behavior Score는 사용자의 카테고리 관심도와 상품 인기도를 결합한 값이다.

```text
Behavior Score

=

categoryInterestScore

+

log(1 + viewCount)
```

예:

```text
13.72
8.41
5.32
```

---

### 7.2 AI Score

AI Score는 Cosine Distance를 유사도 형태로 변환한 값이다.

```text
AI Score

=

1.0 - distance
```

예:

```text
distance = 0.08
score = 0.92
```

---

### 7.3 결합 시 주의사항

Behavior Score와 AI Score는 계산 기준과 Scale이 다르기 때문에 두 점수를 직접 비교해 하나의 통합 Score Ranking을 생성하지 않는다.

현재 결합 정책은 다음과 같다.

```text
AI 결과 우선 배치
        +
Behavior 결과로 부족분 보충
```

다음과 같은 직접 비교는 수행하지 않는다.

```text
AI score 0.91

vs

Behavior score 13.72
```

두 Score를 하나의 Ranking 기준으로 사용하려면 다음과 같은 추가 설계가 필요하다.

- Score 정규화
- AI와 Behavior의 가중치 정책
- 카테고리 및 상품 단위 실험
- CTR 또는 구매 전환 기반 튜닝
- A/B 테스트

---

## 8. recommendationId 정책

Behavior 추천 결과가 생성되는 하나의 추천 API 요청에서는 동일한 `recommendationId`를 사용한다.

```text
Recommendation Request
        ↓
recommendationId 생성
        ↓
Behavior Recommendation
        ↓
AI Recommendation
        ↓
Final Recommendation
```

AI 추천이 적용되더라도 새로운 `recommendationId`를 생성하지 않는다.

이를 통해 최종적으로 사용자에게 노출된 추천 결과를 하나의 추천 요청 단위로 추적할 수 있다.

다만 추천에 필요한 관심 데이터 또는 Candidate가 없어 빈 추천을 반환하는 경우에는 `recommendationId`를 생성하기 전에 종료한다.

따라서 빈 추천 응답은 다음과 같다.

```text
recommendationId = null
items = []
```

---

## 9. Behavior Event

추천 결과의 성능을 측정하기 위해 사용자 행동 이벤트를 수집한다.

| Event | 의미 | recommendationId | 관심도 반영 |
|---|---|---:|---:|
| `IMPRESSION` | 추천 상품 실제 노출 | 필수 | X |
| `CLICK` | 추천 상품 클릭 | 필수 | O |
| `VIEW` | 상품 상세 조회 | 선택 | O |
| `SCRAP` | 상품 찜 | 선택 | O |
| `CART_ADD` | 장바구니 추가 | 선택 | O |
| `PURCHASE` | 구매 완료 | 선택 | O |

추천 성능은 향후 `IMPRESSION`과 `CLICK` 이벤트를 사용하여 CTR로 측정할 수 있다.

```text
CTR

=

Recommendation CLICK

/

Recommendation IMPRESSION
```

`recommendationId`를 사용하면 사용자가 어떤 추천 요청에서 노출된 상품을 클릭하거나 구매했는지 추적할 수 있다.

---

## 10. 예외 및 Fallback 정책

| 상황 | 처리 |
|---|---|
| `limit <= 0` | 잘못된 요청으로 처리 |
| `limit > MAX_RECOMMENDATION_LIMIT` | 잘못된 요청으로 처리 |
| 사용자 관심 데이터 없음 | 빈 추천 결과 반환 |
| Behavior Candidate 없음 | 빈 추천 결과 반환 |
| Behavior 추천 결과 없음 | AI 호출 없이 빈 Behavior 결과 반환 |
| 기준 상품 Vector 없음 | AI 결과 없음으로 처리 후 Behavior 반환 |
| 유사 상품 검색 결과 없음 | Behavior 반환 |
| AI 결과가 모두 판매 불가능 | Behavior 반환 |
| pgvector 또는 AI 처리 예외 | Adapter에서 처리 후 Behavior 반환 |
| AI 결과가 `limit`보다 부족 | Behavior 결과로 보충 |
| AI와 Behavior에 동일 상품 존재 | AI 결과를 우선하고 중복 제거 |
| 병합 결과가 `limit` 초과 | `limit`만큼 잘라서 반환 |

---

## 11. 현재 구현에서 보장하는 사항

### Behavior 추천

- [x] 인증된 사용자 `memberId` 기반 추천 요청
- [x] `UserInterest` 기반 Behavior 추천
- [x] 관심도 상위 3개 카테고리 사용
- [x] `ON_SALE` Behavior Candidate 조회
- [x] 카테고리 관심도 기반 점수 반영
- [x] `log(1 + viewCount)` 기반 인기도 반영
- [x] Behavior Score 내림차순 Ranking
- [x] 요청 `limit` 적용
- [x] AI 호출 전 Behavior 추천 결과 선확보

### AI 추천

- [x] Behavior Top-1 상품을 AI 기준 상품으로 선정
- [x] 기존 Embedding 및 pgvector 유사 상품 추천 시스템 연결
- [x] Cosine Distance 기반 유사 상품 검색
- [x] 최대 Distance 조건 적용
- [x] 기준 상품 자체를 SQL에서 제외
- [x] AI 결과의 [RecommendationItem](cci:2://file:///Users/yanghwayeong/Desktop/beadv7_7_GrafanaOn_BE/recommendation-service/src/main/java/shop/dear/recommendation/behavior/domain/model/RecommendationItem.java:11:0-64:1) 존재 여부 확인
- [x] AI 결과의 `ON_SALE` 상태 검증
- [x] Cosine Distance를 `1.0 - distance` Score로 변환
- [x] AI 예외를 Fallback 가능한 빈 결과로 변환

### 결합 및 Fallback

- [x] AI 결과가 없으면 Behavior 결과 반환
- [x] AI 처리 실패 시 Behavior 결과 반환
- [x] AI 결과를 최종 결과의 앞부분에 배치
- [x] AI 결과가 부족하면 Behavior 결과로 보충
- [x] `productId` 기준 중복 제거
- [x] 중복 시 AI 결과의 위치와 Score 우선
- [x] 최종 `limit` 적용
- [x] 최종 노출 순서 기준 rank 재계산
- [x] Behavior 결과가 생성된 요청에서 동일한 `recommendationId` 유지
- [x] AI Score와 Behavior Score를 직접 비교하여 재정렬하지 않음
- [x] 최대 추천 개수 제한

---

## 12. 현재 구현의 제한사항

### 12.1 AI 후보 초과 조회 미적용

현재는 pgvector에서 `limit`개를 조회한 다음 `ON_SALE` 필터를 적용한다.

따라서 판매 불가능한 상품이 검색 결과에 포함되면 유효한 AI 추천 수가 감소할 수 있다.

최종 추천 개수는 Behavior 결과로 보충하지만, 실제로 사용할 수 있었던 `limit` 이후의 AI 후보를 놓칠 수 있다.

향후 다음 정책을 고려할 수 있다.

```text
AI 후보 조회 개수
=
limit * 2
또는
limit * 3
```

---

### 12.2 Behavior 결과 개수 보장 한계

Behavior Candidate가 요청한 `limit`보다 적으면 Behavior 추천도 요청한 개수를 모두 보장하지 않는다.

AI 결과와 Behavior 결과를 모두 병합한 뒤에도 고유한 상품 수가 부족하면 최종 응답은 `limit`보다 적을 수 있다.

```text
요청 limit = 5

유효 AI 상품 = 1
Behavior 상품 = 2
중복 상품 = 1

        ↓

최종 고유 상품 = 2
```

현재 시스템은 가능한 추천 결과를 반환하며, 항상 정확히 `limit`개를 보장하지는 않는다.

---

### 12.3 Score 정규화 미적용

AI Score와 Behavior Score는 서로 다른 계산 방식을 사용한다.

현재는 우선순위 기반 결합 정책을 사용하며, 두 Score를 정규화하거나 통합하지 않는다.

---

### 12.4 Fallback 범위

AI Adapter 내부 예외는 Behavior Fallback으로 처리한다.

반면 Behavior Context 생성이나 Behavior 추천 자체에서 발생하는 예외는 AI Fallback 대상이 아니며 API 실패로 전파될 수 있다.

Behavior 추천은 기본 추천 경로이므로 별도의 장애 대응 정책이 필요하다면 추가 설계가 필요하다.

---

## 13. 향후 개선 사항

- [ ] `ON_SALE` 필터를 고려한 AI 후보 초과 조회
- [ ] AI Adapter 단위 테스트 보강
- [ ] 기준 상품 Vector 미존재 테스트
- [ ] AI 결과 전체 판매 중지 테스트
- [ ] AI 결과와 Behavior 결과 중복 테스트
- [ ] AI 결과 부족 시 Behavior 보충 테스트
- [ ] AI 예외 발생 시 Behavior Fallback 테스트
- [ ] 최종 결과가 `limit`보다 부족한 상황의 정책 정의
- [ ] AI와 Behavior Score 정규화 연구
- [ ] 추천 전략별 CTR 및 구매 전환율 측정
- [ ] A/B 테스트를 통한 AI 추천 비중 조정
- [ ] AI 추천 Timeout 및 Circuit Breaker 검토
- [ ] 추천 결과 출처를 구분하는 필드 검토
- [ ] 추천 품질 및 Fallback 비율 모니터링

---

## 14. 최종 정책 요약

```text
1. 인증 사용자와 limit으로 추천 API 요청

2. 사용자 관심 카테고리 조회

3. 상위 관심 카테고리의 ON_SALE 상품 조회

4. 관심도와 인기도 기반 Behavior 추천 생성

5. Behavior Top-1 상품을 AI 기준 상품으로 선정

6. pgvector에서 Story Embedding 유사 상품 검색

7. 기준 상품 자체 제외

8. AI 상품의 RecommendationItem 존재 여부 확인

9. AI 상품의 ON_SALE 상태 확인

10. AI 결과가 존재하면 AI를 먼저 배치

11. 부족한 수량은 Behavior 결과로 보충

12. productId 기준 중복 제거

13. 최종 limit 적용 및 rank 재계산

14. AI 결과가 없거나 실패하면 Behavior 결과 반환
```

최종적으로 추천 서비스는 다음 원칙을 따른다.

> Behavior 추천을 안정적인 기본 경로로 사용하고, AI 유사 상품 추천을 선택적인 확장 경로로 결합한다.

> AI 추천 시스템에 문제가 발생하더라도 추천 API의 전체 가용성이 영향을 받지 않도록 Behavior 추천으로 Fallback한다.

> AI Score와 Behavior Score를 직접 비교하지 않고, AI 우선 배치 후 Behavior 결과로 부족한 수량을 보충한다.