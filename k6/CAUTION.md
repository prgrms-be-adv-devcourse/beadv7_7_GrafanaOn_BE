# 고려해야 할 사항

스크립트를 돌리기 전/후에 판단이 필요한 것들만 남겼다.
9개 시나리오는 2026-08-09 기준 3 VU 스모크 테스트에서 전부 100% 통과했다.

---

## 1. 결제 API — 절대 부하 금지 🚨

`POST /api/payments/confirm`은 `TossPaymentApprovalAdapter`를 통해 **실제 토스 API를 호출**한다.
부하테스트에 넣으면 외부 PG사에 트래픽을 쏘는 행위가 된다. `financial.js`에서 제외했다.

`POST /api/payments/charge`도 미완료 결제 레코드가 계속 쌓여서 제외했다.

결제 경로 성능이 필요하면 **PG를 모킹한 환경**에서 별도로 측정할 것.
누가 시나리오를 추가하더라도 이 두 엔드포인트는 넣지 말 것.

---

## 2. 상품을 ON_SALE로 바꾸는 API가 없다 🚨 (앱 이슈)

`POST /api/products`로 만든 상품은 `status=PREPARING`으로 생성된다.
그런데 이를 `ON_SALE`로 전환하는 경로가 **프로덕션 코드에 존재하지 않는다.**
`Product.changeStatusToOnSale()`은 테스트 코드에서만 호출된다.

`PREPARING` 상품은 사실상 아무것도 할 수 없다.

| 시도 | 결과 |
|---|---|
| `POST /api/offers/snapshot` | `OF-008` 상품 정보를 조회하지 못했습니다 |
| `POST /api/purchases` | 동일하게 실패 |
| `GET /api/products/{id}` (남이 조회) | 실패 — `PREPARING`은 판매자 본인에게만 보임 |

**부하테스트 영향**
- `scenarios/seed.js`(k6, HTTP)로 만든 상품은 전부 PREPARING이라 오퍼/구매에 쓸 수 없다.
  → `sql/activate-products.sql`로 상태를 바꿔야 한다.
- `sql/seed-bulk.sql`은 처음부터 `ON_SALE`로 넣으므로 문제없다.
- 모든 조회 시나리오의 `setup()`은 `?status=ON_SALE`로 필터링하도록 수정했다.

**팀에 확인이 필요하다.** 실서비스에서 상품이 어떻게 판매중이 되는지 설계가 빠진 것인지,
아직 구현 전인지 확인할 것. 부하테스트와 별개로 기능 공백이다.

---

## 3. 오퍼는 한 구매자가 한 상품에 1회만 가능하다

스냅샷이 `(writerId, productId)`로 재사용되는데, 이미 오퍼에 연결된 스냅샷은 다시 쓸 수 없다.
→ `OFS-001` 이미 오퍼에 연결된 스냅샷입니다.

`offer.js`는 VU별 계정이 고정이므로 상품을 **순차로** 고르도록 했다(무작위로 고르면 중복 발생).

⚠️ **VU당 반복 횟수가 ON_SALE OFFER 상품 수를 넘으면 실패한다.**
`seed-bulk.sql` 기본값(상품 30,000 중 OFFER 절반)이면 충분하지만, 소규모로 시딩했다면
반복 횟수를 상품 수 이하로 제한할 것.

---

## 4. 측정에서 빠진 엔드포인트 (order 담당 판단 필요)

**`GET /api/offers/products/{productId}` — 판매자 전용**

상품 소유자만 호출할 수 있다(`OF-004 NOT_OFFER_SELLER`). 구매자 흐름에 넣을 수 없다.
판매자 계정으로 상품을 새로 만들어 우회하려 했으나, 위 2번(PREPARING) 때문에 불가능했다.

오퍼가 쌓일수록 무거워지는 구간이라 성능상 중요한데 현재 측정되지 않는다.
`seed-bulk.sql`로 ON_SALE 상품을 만든 뒤, 그 판매자 계정으로 로그인할 수 있게 하면 측정 가능하다
(현재 벌크 시딩 계정은 더미 비밀번호 해시라 로그인 불가).

**`PATCH /api/offers/{id}/accept`, `/reject`**

- 상태를 되돌릴 수 없어 반복 실행 불가 (오퍼 1건당 1회)
- 판매자 세션 필요
- 수락 시 상품이 판매완료가 되어 대상이 빠르게 고갈

결제·정산까지 연쇄되는 무거운 경로라 중요한데 측정되지 않고 있다.
필요하면 별도의 단발성 스크립트로 분리해야 한다.

---

## 5. 목표 TPS를 재려면 THINK_TIME=0 이어야 한다

`TPS = VU / (응답시간 + THINK_TIME)`

- 50 VU + 100ms 응답 + `THINK_TIME=1`(기본) → 약 **45 TPS**
- 50 VU + 100ms 응답 + `THINK_TIME=0` → 약 **500 TPS** (목표치)

기본값 1초는 실사용자 재현용이라, 그대로 두면 목표 500 TPS에 절대 도달하지 않는다.
**두 값을 모두 돌려서 "실사용 응답시간"과 "최대 처리량"을 나눠 볼 것.**
결과를 공유할 때 어느 설정으로 측정한 것인지 반드시 명시해야 한다.

---

## 6. 구매(write) — 상품이 1회용이라 반복 측정 불가

구매 1건당 IMMEDIATE 상품 1개가 판매완료로 소모되어 재사용할 수 없다.
load/stress 단계(50~200 VU × 수 분)로는 수천 개의 상품이 필요해 비현실적이다.

**현재 대응**: `MODE=write`이면 `purchase.js`가 자동으로 burst 단계(VU 10 / 30초 ≈ 90건)로
전환된다. 구매 API의 응답시간 자체는 이걸로 측정 가능하다.

**남은 판단거리**
- 고부하(50 VU 이상)에서의 구매 성능을 봐야 한다면 상품을 수천 개 시딩하거나,
  테스트 중 별도 세션에서 상태를 되돌리는 SQL을 주기적으로 돌려야 한다. 팀 합의 필요.
- 취소(cancel)로 상품이 `ON_SALE`로 복구되는지는 도메인 정책에 달려 있다. 소규모로 먼저
  돌려서 실제 소모량을 확인할 것.

---

## 7. 지갑 잔액은 DB 직접 시딩에 의존한다

잔액을 채우는 API가 없다.
- `POST /internal/deposits` — 지갑 "생성"만 하고 잔액은 0
- `POST /api/payments/confirm` — 실제 토스 호출이라 사용 금지

그래서 `sql/seed-wallet.sql`로 DB에 직접 넣는다. 이때 두 가지 부수효과가 있다.

- **`wallet_log`에 TOPUP 기록이 남지 않는다.** 잔액과 로그가 불일치하므로, 정산/이력
  조회 결과까지 함께 검증하려면 SQL 하단의 주석 처리된 로그 INSERT도 같이 실행할 것.
- **실행 중 생성되는 계정은 시딩 대상이 될 수 없다.** memberId를 미리 알 수 없기 때문.
  그래서 구매 시나리오만 `seed.js`가 만든 고정 계정(`loadtest-buyer-N@example.com`)을 쓴다.
  `BUYER_COUNT`는 반드시 구매 테스트의 최대 VU 수 이상이어야 한다(기본 10 = burst VU와 동일).

또한 identity(`auth_account`)와 commerce(`wallet`)가 **같은 DB를 쓰는 현재 구조**를 전제로
조인한다. 서비스별 DB가 분리되면 SQL을 고쳐야 한다.

---

## 8. 부하테스트는 Cloudflare를 우회해야 한다

`api.d-ear.shop`은 Cloudflare 프록시 뒤에 있다. 이 경로로 부하를 쏘면 안 된다.

- **측정 오염** — 엣지 경유 지연(관측 시 홍콩 노드)이 섞여 우리 서버 성능과 구분되지 않는다
- **차단 위험** — DDoS 방어에 걸려 실패율이 치솟거나 팀 IP가 차단될 수 있다
- **약관** — CF 네트워크를 통한 부하테스트는 일반적으로 사전 협의가 필요하다

따라서 **오리진 IP 직접 호출**(`http://<EC2_IP>:8080`)로 측정한다. 보안그룹은 테스트 때만 열고 닫을 것.

이때 부작용이 하나 있다. 로그인이 내려주는 Refresh Token 쿠키에 `Secure` 플래그가 붙어 있어
(`AUTH_COOKIE_SECURE=true`) 평문 HTTP에서는 k6가 쿠키를 돌려보내지 않는다.
`auth-flow.js`는 로그인 응답에서 토큰 값을 직접 꺼내 `Cookie` 헤더로 실어 보내도록 처리했다.
**서버 버그가 아니라 테스트 환경 때문에 생긴 문제다.**

---

## 9. 대량 시딩 SQL 실행 시 주의

컬럼명은 엔티티의 `@Column(name = ...)`을 그대로 따랐고, 명시가 없는 필드
(`offer.number`, `purchase.amount` 등)는 전부 단일 단어라 암묵적 네이밍으로도
같은 이름이 된다. **컬럼명 자체는 신뢰해도 된다.**

- **`ddl-auto: update`는 컬럼을 추가만 하고 삭제·이름변경을 하지 않는다.** 과거 스키마의
  잔재가 NOT NULL로 남아 있으면 INSERT가 실패할 수 있다. 처음에는 **작은 값으로 먼저** 돌려볼 것.

  ```bash
  psql ... -v member_count=10 -v product_count=100 -v offer_count=20 \
           -v purchase_count=10 -f sql/seed-bulk.sql
  ```

- **`search_product` 미러링을 빠뜨리면 검색 테스트가 무의미해진다.**
  검색 API는 `product`가 아니라 `search_product`를 읽는다. 평소에는 상품 등록
  이벤트(`ProductChangedEvent`)로 채워지지만 SQL로 직접 넣으면 이벤트가 발생하지 않는다.
  `seed-bulk.sql` 6단계가 이 미러링을 하는데, 실패하면 검색이 전부 빈 결과를 반환하면서
  "엄청 빠르다"는 잘못된 결론이 나온다. 시딩 후 확인할 것:

  ```sql
  SELECT COUNT(*) FROM search_product WHERE model_number LIKE 'BULK-%';
  ```

참고로 `member.withdrawnAt`은 `@Column(name = "withdrawnAt")`이 카멜케이스라
Postgres에서 `withdrawnat`으로 생성된다. nullable이라 이 SQL에는 영향이 없지만
직접 쿼리를 짤 때는 주의할 것.

---

## 10. 시딩 순서 — 부하테스트 계정이 남아 있는 상태에서 시딩하지 말 것

`seed-bulk.sql`은 상품을 판매자에게 배분하는데, **초기 버전이 `seller` 테이블 전체에서
골랐다.** 그런데 부하테스트로 만들어진 `loadtest-%` 계정도 판매자로 등록되어 그 테이블에
남아 있어서, BULK 상품 일부가 런타임 계정 소유로 만들어졌다.

그 상태에서 `cleanup-runtime.sql`을 돌리면
- 소유자(런타임 계정)는 삭제되고
- 상품은 `BULK-%`라 남아서 **소유자 없는 상품**이 되고
- 그 상품에 달린 BULK 오퍼는 `seller_id` 조건에 걸려 **함께 삭제**된다

2026-08-09에 실제로 발생했다: 상품 2,607개 고아 + BULK 오퍼 880건 손실(9,997 → 9,117).

**현재는 수정됨** — `bulk_sellers` 임시 테이블로 bulk 판매자만 골라 쓰고, 시딩 끝에
정합성 확인 쿼리가 자동으로 돈다. 다만 원칙은 지킬 것:

- 시딩 전에 `cleanup.sql`로 기존 부하테스트 계정을 먼저 정리한다
- 시딩 후 "정합성 확인" 결과가 **전부 0인지** 반드시 본다

이미 고아가 생겼다면 살아있는 bulk 판매자에게 재배정한다.

```sql
UPDATE product
SET seller_id = (
    SELECT s.member_id FROM seller s
    JOIN member m ON m.id = s.member_id
    WHERE m.nickname LIKE 'bulk\_user\_%'
    ORDER BY s.member_id LIMIT 1
)
WHERE model_number LIKE 'BULK-%'
  AND NOT EXISTS (SELECT 1 FROM member m2 WHERE m2.id = product.seller_id);
```

---

## 11. 재실행 시 유니크 제약 충돌

DB에 이전 실행 데이터가 남아 있어서, 값이 결정적이면 재실행 시 충돌한다.

- `member.nickname` — `member.js`는 실행 단위 `RUN_ID`를 붙여 회피한다
- `auth_account.email` — 타임스탬프를 붙여 회피한다
- `cart_item (cart_id, product_id)` — 매 반복 끝에 장바구니를 비워 회피한다

새 시나리오를 추가할 때 유니크 제약이 있는 컬럼을 쓰면 같은 처리가 필요하다.
누적된 테스트 데이터는 `sql/cleanup.sql`, `sql/cleanup-bulk.sql`로 정리한다.

---

## 12. 페이징 규칙이 API마다 다르다

| API | page 시작 | 검증 |
|---|---|---|
| `GET /api/search/products` | **1** | `@Min(1)` — 0을 보내면 실패 |
| `GET /api/scraps` | **0** | 없음 |

검색만 1-based다(`page - 1`로 내부 변환). 스크립트는 각각에 맞춰 놨지만,
새 시나리오를 추가할 때 해당 컨트롤러의 규칙을 확인할 것.

API 일관성 관점에서는 통일하는 게 맞아 보이지만, 이건 팀 논의 사항이다.

---

## 13. `sql/cleanup.sql`은 FK 관계 미검증

`product`를 참조하는 테이블(cart_item, offer, offer_snapshot, purchase 등)의 FK 관계를
전부 확인하지 않았다. 실행하면 FK 위반이 날 수 있고, 그러면 참조하는 쪽부터 지우도록
순서를 조정해야 한다.

- 실행 전 SQL 상단의 **삭제 대상 확인 SELECT를 먼저** 돌릴 것
- **운영 DB에서는 절대 실행 금지**

---

## 14. Grafana 연동 미완

k6 결과를 Grafana에서 실시간으로 보려면 `xk6-output-prometheus-remote-write` 확장 빌드가
필요하다. 기본 `grafana/k6` 이미지에는 포함되어 있지 않다.

현재는 콘솔 summary로만 확인 가능하다. 그전까지는 **부하 도중 서버 CPU/메모리를 별도로 기록해 둘 것.**

또한 Spring 메트릭을 Alloy로 수집하려면 `/actuator/prometheus`가 열려 있어야 하는데,
현재 gateway 설정은 `management.endpoints.web.exposure.include: health,info,gateway`뿐이라
`prometheus`가 빠져 있다. 모니터링 붙일 때 함께 손봐야 한다.

---

## 15. 로그인 p95 100ms 기준은 재검토 필요

`POST /api/auth/login`은 BCrypt 해시 비교가 들어가서 의도적으로 느리다(보안상 정상).
전역 threshold인 `p(95)<100`을 못 맞출 가능성이 높은데, 이건 성능 결함이 아니다.

실측 참고치(1 VU, 서울 리전 EC2 오리진 직접, 네트워크 포함):
- 최소 29ms (로그아웃), 최대 194ms (회원가입 — BCrypt 포함), 중앙값 101ms

**네트워크 왕복이 포함된 상태에서 목표 100ms는 빠듯하다.** 로그인·회원가입만 별도 기준
(예: p95 < 500ms)을 적용할지 팀에서 합의할 것. 현재는 전 시나리오에 동일한 threshold가 걸려 있다.
