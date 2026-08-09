# k6 부하테스트

D:EAR 서비스의 BC별 부하테스트 스크립트.
로컬 PC에서 Docker로 k6를 띄우고, 컨테이너 리소스를 t3.large 스펙(2 vCPU / 8GB)에 맞춘다.

## 디렉토리

```
config/
  environment.js   TARGET/URL/THINK_TIME/THRESHOLDS
  stages.js        load / stress / burst VU 단계
  auth.js          로그인·인증헤더·판매자등록 공용 헬퍼
scenarios/
  seed.js          기초 데이터 생성 (최초 1회, 공용)
  auth-flow.js     Auth
  search.js        Search
  member.js        Member (프로필 + 판매자)
  scrap.js         Scrap
  product.js       Product
  cart.js          Cart
  offer.js         Offer
  purchase.js      Purchase
  financial.js     Wallet / Settlement
sql/
  seed-wallet.sql  구매 테스트용 지갑 잔액 시딩
  cleanup.sql      테스트 데이터 정리
```

---

# 역할별 실행 가이드

## 공통 선행 작업 (한 명이 대표로 1회만 실행)

### 데이터 기준

측정 결과를 비교하려면 **모두가 같은 데이터 규모 위에서** 재야 한다. 기준값은 아래와 같다.

| 항목 | 기준값 | 변수명 | 왜 이 규모인가 |
|---|---|---|---|
| 회원 | 1,000 | `member_count` | 닉네임 중복검사 쿼리에 부하를 주는 최소 규모 |
| 판매자 비율 | 10% | `seller_ratio` | 회원 1,000명 중 100명이 판매자 |
| 상품 | 30,000 | `product_count` | 인덱스/쿼리플랜이 유의미해지는 최소 규모 |
| 상품당 이미지 | 3 | `images_per_product` | 상세조회 N+1 노출용 |
| 오퍼 | 10,000 | `offer_count` | 상품별 오퍼 목록 조회 부하 |
| 오퍼 집중 비율 | 10% | `hot_product_ratio` | 인기상품 편중 재현 (전체 오퍼가 상품 10%에 몰림) |
| 구매 이력 | 5,000 | `purchase_count` | 내 구매목록·정산 집계 부하 |
| 정산 기간 | 12개월 | `settlement_months` | 기간 조회 부하 |
| 회원당 장바구니 | 15 | `cart_items_per_member` | 장바구니 조회 시 상품 매핑 비용 |

> **왜 이만큼 필요한가**: 상품 200개짜리 테이블은 PostgreSQL이 통째로 메모리에 올려
> seq scan으로 긁는다. 인덱스를 타는지가 전혀 드러나지 않아 "전부 p95 20ms" 같은
> 무의미한 결과가 나온다. LIKE 검색·N+1·기간 집계 같은 실제 병목은 데이터가 쌓여야 보인다.

### 1단계 — 대량 배경 데이터 (SQL)

```bash
psql -h <DB_HOST> -U <USER> -d dear -f sql/seed-bulk.sql
```

기준값을 바꾸려면 `-v 변수명=값`으로 덮어쓴다. **여러 개를 동시에 줄 수 있고,
안 준 변수는 위 표의 기준값이 그대로 적용된다.**

```bash
# 예: 가볍게 돌려보기 (상품 5천 개)
psql -h <DB_HOST> -U <USER> -d dear \
  -v member_count=200 \
  -v product_count=5000 \
  -v offer_count=1000 \
  -v purchase_count=500 \
  -f sql/seed-bulk.sql
```

```bash
# 예: 더 크게 (상품 10만 개)
psql -h <DB_HOST> -U <USER> -d dear \
  -v product_count=100000 \
  -v offer_count=50000 \
  -f sql/seed-bulk.sql
```

⚠️ **HTTP가 아니라 SQL로 넣는 이유**: 상품 3만 개를 k6로 등록하면 POST 3만 번이라
수십 분이 걸리고 그 자체가 부하테스트가 된다. 배경 데이터는 SQL 벌크로, 실제로
로그인해서 요청을 보내는 계정만 k6로 만든다.

### 2단계 — 테스트 계정 (k6)

```bash
cd /Users/ryu/Desktop/LoadTestScript
docker run --rm -v "$PWD:/scripts" -w /scripts \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/seed.js
```

1단계로 만든 회원은 비밀번호 해시가 더미라 **로그인할 수 없다**(의도된 동작 — 볼륨용).
실제로 요청을 보낼 계정은 이 단계에서 만든다. 출력되는 `-e PRODUCT_IDS=...` 값을
팀에 공유하면 각자 붙여 쓰면 된다. 생략해도 각 시나리오가 `GET /api/products`로
자동 조회하므로 대개 그냥 돌려도 된다.

### 3단계 — 상품 판매중 전환 (오퍼/구매 테스트를 한다면 필수)

```bash
psql -h <DB_HOST> -U <USER> -d dear -f sql/activate-products.sql
```

⚠️ `POST /api/products`로 만든 상품은 `PREPARING` 상태로 생성되는데, 이를 `ON_SALE`로
바꾸는 API가 **존재하지 않는다**(CAUTION 2번). `PREPARING` 상품은 오퍼·구매 대상이 될 수
없고 남에게 조회되지도 않으므로, 2단계 `seed.js`로 만든 상품을 쓰려면 이 SQL이 필요하다.
1단계 `seed-bulk.sql`로 만든 상품은 처음부터 `ON_SALE`이라 불필요하다.

### 정리

```bash
psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup-bulk.sql   # 1단계 배경 데이터
psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup.sql        # 2단계 k6 테스트 계정
```

> **팀 협업 규칙 (중요)**
>
> | 작업 | 누가 | 언제 |
> |---|---|---|
> | 배경 데이터 시딩 (1~4단계) | **대표 1명** | 최초 1회만 |
> | 부하테스트 실행 | 각자 | **순번대로, 절대 동시 실행 금지** |
> | 런타임 데이터 정리 (`cleanup-runtime.sql`) | 각자 | 본인 테스트 직후 |
> | 전체 정리 (`cleanup-bulk.sql`) | 대표 1명 | 모든 일정 종료 후 |
>
> **① 배경 데이터는 공유 자산이다.** 상품 30,000개는 "실서비스 1년치" 기준선이고,
> 모두가 같은 데이터 위에서 재야 결과를 비교할 수 있다. 사람마다 지웠다 다시 넣으면
> 매번 다른 DB에서 측정하는 셈이라 비교가 무의미해진다.
>
> **② 동시 실행은 금지.** 서버가 1대라 두 명이 동시에 50 VU를 쏘면 CPU를 나눠 쓰게 되고
> **양쪽 결과가 모두 무효**가 된다. "지금 누가 돌리는 중"을 팀에 공유할 것.
>
> **③ 테스트 후에는 런타임 데이터를 정리한다.** 특히 `auth-flow`는 매 반복 계정을
> 새로 만들어서, 50 VU 10분이면 6,000개 이상이 쌓인다. 그대로 두면 다음 사람은
> 회원 1,000명이 아니라 7,000명인 DB에서 측정하게 된다.
> ```bash
> psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup-runtime.sql
> ```
> 이 SQL은 배경 데이터(`bulk_user_%`, `BULK-%`)와 구매용 고정 계정
> (`loadtest-buyer-N`, 지갑 잔액 포함)은 **남기고** 런타임 생성분만 지운다.

> **공통 측정 원칙**
> 모든 역할이 아래 3가지를 각각 돌려서 비교한다.
> 1. `TARGET=direct` — 내 서비스 단독 응답시간 (기준선)
> 2. 기본(gateway) — 실제 사용자 경로. 1번과의 차이 = 게이트웨이 오버헤드
> 3. `THINK_TIME=0` — 목표 500 TPS 도달 여부
>
> 부하 도중 Grafana에서 해당 서비스 컨테이너의 CPU/메모리를 함께 기록할 것.

---

## 1. cart 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/cart.js` |
| 대상 API | `POST /api/carts/items`, `GET /api/carts`, `DELETE /api/carts/items`, `DELETE /api/carts/items/all` |
| 선행 조건 | 상품 시딩 (`seed.js`) |
| 흐름 | 담기 → 조회 → 선택 삭제 → 전체 비우기 (자기정리형) |

```bash
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/cart.js
```

**중점 관찰 포인트**
- `GET /api/carts`가 가장 무거울 가능성이 높다. 장바구니 항목마다 상품 정보를 매핑하는
  구간(productId Map 조회)이 있어서, 항목 수가 늘면 N+1이 드러날 수 있다.
- CartItem 유니크 제약 때문에 같은 상품 중복 담기는 실패한다. 매 반복 끝에 비우도록
  짜여 있으니 `http_req_failed`가 오르면 정리 로직이 제대로 도는지 먼저 확인할 것.

---

## 2. financial 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/financial.js` |
| 대상 API | `GET /api/deposits/me`, `GET /api/settlements/me`, `GET /api/settlements/me/history` |
| 선행 조건 | 없음 (조회 전용) |
| 흐름 | 지갑 잔액 → 정산 예정금액 → 정산 이력 |

```bash
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/financial.js
```

**중점 관찰 포인트**
- `GET /api/settlements/me/history`가 기간 조회라 데이터가 쌓일수록 느려진다.
  인덱스 유무를 확인할 것. 지금은 시드 데이터가 적어 빠르게 나올 수 있으니,
  실제 정산 데이터를 쌓은 뒤 다시 측정하면 더 의미 있다.
- `GET /api/settlements/me`는 전월 1일~말일 집계라 쿼리 비용이 다르다. 두 API를
  `tags.name`으로 나눠 보면 차이가 드러난다.

**⚠️ 절대 하면 안 되는 것**
- `POST /api/payments/confirm`은 `TossPaymentApprovalAdapter`를 통해 **실제 토스 API를
  호출**한다. 부하테스트에 절대 포함하지 말 것. 그래서 `financial.js`에서 제외했다.
- `POST /api/payments/charge`도 미완료 결제가 계속 쌓여서 제외했다.
- 결제 경로 성능을 재야 한다면 PG를 모킹한 환경에서 별도로 진행할 것.

---

## 3. member 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/member.js`, `scenarios/scrap.js` |
| 대상 API | `GET/PATCH /api/members/profile*`, `GET/PATCH /api/members/me/seller`, `POST/GET/DELETE /api/scraps` |
| 선행 조건 | scrap은 상품 시딩 필요 |

```bash
# 프로필 + 판매자
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/member.js

# 스크랩
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/scrap.js
```

**중점 관찰 포인트**
- `PATCH /api/members/me/seller`는 계좌번호 **암호화**, `GET /api/members/me/seller`는
  **마스킹** 처리가 들어간다. 암호화 비용이 응답시간에 얼마나 잡히는지 두 API를 비교할 것.
- `PATCH /api/members/profile/me`는 닉네임 중복 검사(`existsByNickname`) 쿼리가 매번 돈다.
  회원 수가 늘면 여기가 병목이 될 수 있다.
- 판매자 등록(`POST /me/seller`)은 중복 호출 시 `ALREADY_SELLER`로 실패하므로
  VU별 1회만 호출하도록 짜여 있다. 이 요청은 `SETUP` 태그로 분리 집계된다.

---

## 4. order 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/offer.js`, `scenarios/purchase.js` |
| 대상 API | `POST /api/offers/snapshot`, `POST /api/offers`, `GET /api/offers/*`, `GET/POST/DELETE /api/purchases*` |
| 선행 조건 | offer는 OFFER 상품, purchase write는 IMMEDIATE 상품 + **지갑 잔액 SQL 시딩** |

```bash
# 오퍼 (스냅샷 → 등록 → 조회)
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  -e PRODUCT_IDS=<seed.js가 출력한 OFFER 목록> \
  grafana/k6 run scenarios/offer.js

# 구매 조회 (기본, 반복 안전)
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/purchase.js
```

**구매 쓰기 경로 (준비 3단계 필요)**

```bash
# 1) 시딩 — 상품 + 구매용 고정 계정 생성
docker run --rm -v "$PWD:/scripts" -w /scripts \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/seed.js

# 2) 지갑 잔액 충전 (충전 API가 없어 DB 직접 시딩)
psql -h <DB_HOST> -U <USER> -d dear -f sql/seed-wallet.sql

# 3) 구매 write — burst 단계로 자동 전환됨 (VU 10 / 30초 ≈ 90건)
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  -e MODE=write \
  -e PRODUCT_IDS=<seed.js가 출력한 IMMEDIATE 목록> \
  grafana/k6 run scenarios/purchase.js
```

**중점 관찰 포인트**
- `POST /api/offers/snapshot` → `POST /api/offers`는 2단계 체인이다. 스냅샷 생성이
  느리면 전체 오퍼 등록 흐름이 같이 느려지므로 두 태그를 나눠 볼 것.
- `GET /api/offers/products/:productId`는 상품당 오퍼가 쌓일수록 무거워진다.
- 구매 생성은 지갑 차감 + 결제 이벤트 발행이 얽혀 있어 트랜잭션이 가장 길다.
  `POST /api/purchases`의 p95를 다른 API와 비교해볼 것.

**⚠️ 제약**
- 구매 1건당 IMMEDIATE 상품 1개가 판매완료로 소모된다. 그래서 load/stress 단계가 아니라
  burst(VU 10 / 30초)로 자동 전환된다. 상품이 부족하면 `seed.js`를 다시 돌릴 것.
- `PATCH /api/offers/{id}/accept`, `/reject`는 시나리오에서 제외했다. 1회성이라
  반복 불가하고, 판매자 세션이 따로 필요하며, 수락 시 상품이 소모된다.

---

## 5. product 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/product.js` |
| 대상 API | `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/me`, `POST/PATCH/DELETE /api/products` |
| 선행 조건 | read 모드는 상품 시딩 필요, write 모드는 없음 |
| 모드 | `MODE=read`(기본) / `MODE=write` |

```bash
# 조회 (실제 트래픽 대부분을 차지하는 경로)
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/product.js

# 쓰기 (등록 → 내 상품 → 수정 → 삭제. 삭제로 자기정리)
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  -e MODE=write \
  grafana/k6 run scenarios/product.js
```

**중점 관찰 포인트**
- `GET /api/products`는 공개 API라 인증 없이 들어온다. 카테고리 필터 조합에 따라
  인덱스가 타는지 확인할 것. 트래픽이 가장 몰릴 API다.
- `GET /api/products/{id}` 상세는 이미지·스토리를 함께 로딩하므로 N+1 후보다.
- write 모드는 판매자 검증(`memberPort.isSeller`)이 매번 identity 쪽을 호출한다.
  이 구간이 `POST /api/products` 응답시간에 얼마나 기여하는지가 핵심 관찰 대상이다.
  → `TARGET=direct`로 commerce 단독 측정을 함께 하면 분리해서 볼 수 있다.

---

## 6. search + gateway 담당

| 항목 | 내용 |
|---|---|
| 스크립트 | `scenarios/search.js`, `scenarios/auth-flow.js` |
| 대상 API | `GET /api/search/products`, `POST /api/auth/{signup,login,reissue,logout}` |
| 선행 조건 | search는 상품 시딩 필요 (검색 결과가 잡히도록) |

```bash
# 검색
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/search.js

# 인증 흐름
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/auth-flow.js
```

**게이트웨이 오버헤드 측정 (이 역할의 고유 업무)**

게이트웨이는 모든 요청이 지나가는 단일 관문이라, 여기가 느리면 전 서비스가 느려진다.
**같은 스크립트를 두 번 돌려 차이를 재는 것이 핵심 산출물**이다.

```bash
# A. 게이트웨이 경유
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e GATEWAY_URL=http://<EC2_IP>:8080 \
  grafana/k6 run scenarios/search.js

# B. commerce 단독
docker run --rm -v "$PWD:/scripts" -w /scripts --cpus="2" --memory="8g" \
  -e TARGET=direct -e COMMERCE_URL=http://<EC2_IP>:8082 \
  grafana/k6 run scenarios/search.js
```

`A의 p95 − B의 p95` = 게이트웨이 오버헤드. 다른 담당자들의 시나리오에도 같은 방식을
적용해서 "게이트웨이 때문에 느린 것"과 "서비스 자체가 느린 것"을 분리해 줄 것.

**중점 관찰 포인트**
- 검색은 `type`에 따라 쿼리가 다르다. `PRODUCT_NAME`/`STORY_CONTENT`는 LIKE `%keyword%`라
  인덱스를 못 타고, `CATEGORY`는 정확히 일치다. 태그가 하나로 묶여 있으니 세 타입을
  분리해서 보고 싶으면 `tags.name`에 type을 넣어 쪼갤 것.
- 게이트웨이는 매 요청마다 JWT 서명 검증(HS256)을 한다. VU가 올라갈 때 게이트웨이
  컨테이너의 CPU가 어떻게 움직이는지 Grafana에서 함께 볼 것.
- `auth-flow.js`는 다른 시나리오와 달리 **매 반복 새 계정을 만든다**(가입 API 자체가
  측정 대상이므로). 그만큼 DB에 계정이 빠르게 쌓이니 `sql/cleanup.sql`로 주기적으로 정리할 것.
- 로그인은 BCrypt 해시 비교가 들어가서 의도적으로 느리다. p95 100ms 기준을 못 맞출
  가능성이 높은데, 이건 보안상 정상이므로 별도 기준을 잡는 것이 맞다.

---

# 실행 방법 (공통)

> ⚠️ 스크립트가 `../config/*.js`를 import하므로 **stdin 파이프(`k6 run - < file.js`)로는
> 동작하지 않는다.** k6가 상대경로의 기준을 잡지 못한다. 반드시 디렉토리를 볼륨으로 마운트할 것.

## 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `TARGET` | `gateway` | `gateway`(게이트웨이 경유) / `direct`(서비스 직접) |
| `GATEWAY_URL` | `http://localhost:8080` | 게이트웨이 주소 |
| `IDENTITY_URL` | `http://localhost:8081` | identity-service (direct 모드) |
| `COMMERCE_URL` | `http://localhost:8082` | commerce-service (direct 모드) |
| `TEST_TYPE` | `load` | `load` / `stress` |
| `THINK_TIME` | `1` | 요청 간 대기(초). 최대 처리량 측정은 `0` |
| `MODE` | `read` | product / purchase의 `read` / `write` |
| `PRODUCT_IDS` | (자동조회) | 대상 상품 ID 목록 |
| `BUYER_MEMBER_IDS` | — | direct 모드로 구매 write 테스트할 때만 필요 |

## VU 단계

| TEST_TYPE | 단계 |
|---|---|
| `load` (기본) | 1 → 10 → 20 → 30 → 40 → 50 VU (각 10초 램프업 + 1분 30초 유지) |
| `stress` | 50 → 100 → 150 → 200 VU |
| burst | VU 10 / 30초 — `purchase.js MODE=write`에서 자동 적용 |

## 목표 TPS와 THINK_TIME

TPS는 `VU / (응답시간 + THINK_TIME)`으로 결정된다.

- 50 VU + 100ms 응답 + `THINK_TIME=1` → **약 45 TPS**
- 50 VU + 100ms 응답 + `THINK_TIME=0` → **약 500 TPS** (목표치)

**목표 500 TPS를 검증하려면 `THINK_TIME=0`으로 돌려야 한다.** 기본값 1초는 실사용자의
생각하는 시간을 재현한 값이라, 그대로 두면 목표 TPS에 도달하지 않는다.
둘 다 돌려서 "실사용 패턴에서의 응답시간"과 "최대 처리량"을 나눠 보는 것을 권장한다.

## 데이터 정리

```bash
psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup-bulk.sql   # 대량 배경 데이터
psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup.sql        # k6 테스트 계정
```

실행 전 각 파일 상단의 주의사항을 반드시 읽을 것. **운영 DB에서는 절대 실행 금지.**
