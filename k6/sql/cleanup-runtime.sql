-- ===========================================================================
-- 부하테스트 "런타임 생성 데이터"만 정리 (기준선 복원용)
--
-- 언제 쓰는가:
--   각자 자기 시나리오를 돌린 직후. 다음 사람이 같은 조건에서 측정할 수 있도록
--   테스트 중 생긴 데이터를 지우고 기준선으로 되돌린다.
--
-- 무엇을 지우고 무엇을 남기는가:
--   지움  ─ 테스트 실행 중 생성된 계정/오퍼/상품 (VU가 만든 것)
--   남김  ─ sql/seed-bulk.sql로 만든 배경 데이터 (bulk_user_%, BULK-%)
--   남김  ─ seed.js가 만든 구매용 고정 계정 (loadtest-buyer-N)과 그 지갑 잔액
--           → 이걸 지우면 purchase write 테스트 전에 매번 재시딩해야 한다
--
-- 실행:
--   psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup-runtime.sql
--
-- 전체 정리(배경 데이터까지)는 sql/cleanup-bulk.sql + sql/cleanup.sql 을 쓴다.
--
-- ⚠️ 운영 DB에서는 절대 실행하지 말 것.
-- ===========================================================================

-- ── 삭제 대상 확인 ───────────────────────────────────────────────────
\echo '=== 삭제 대상 (런타임 생성분) ==='
SELECT 'auth_account(런타임)' AS target, COUNT(*) FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
  AND email NOT LIKE 'loadtest-buyer-%'
UNION ALL
SELECT 'product(런타임 LT-)', COUNT(*) FROM product WHERE model_number LIKE 'LT-%'
UNION ALL
SELECT 'product(offer 타깃)', COUNT(*) FROM product WHERE model_number LIKE 'OFFER-TARGET-%';

\echo ''
\echo '=== 유지되는 데이터 ==='
SELECT 'member(배경 bulk_)' AS keep, COUNT(*) FROM member WHERE nickname LIKE 'bulk\_user\_%'
UNION ALL
SELECT 'product(배경 BULK-)', COUNT(*) FROM product WHERE model_number LIKE 'BULK-%'
UNION ALL
SELECT 'auth_account(구매용 고정)', COUNT(*) FROM auth_account WHERE email LIKE 'loadtest-buyer-%';


BEGIN;

-- 런타임 계정의 member_id를 먼저 확보한다 (buyer 고정 계정은 제외).
CREATE TEMP TABLE runtime_member_ids AS
SELECT member_id
FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
  AND email NOT LIKE 'loadtest-buyer-%'
  AND member_id IS NOT NULL;

-- ── 구매용 고정 계정이 만든 데이터 ─────────────────────────────────
--
-- loadtest-buyer-N 계정은 지갑 잔액이 시딩되어 있어 계정 자체는 유지한다.
-- 다만 그 계정으로 만든 구매·결제는 테스트 산물이므로 지워야 한다.
-- (single/purchase/*, scenarios/purchase.js MODE=write 가 만든다)
CREATE TEMP TABLE loadtest_actor_ids AS
SELECT member_id FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
  AND member_id IS NOT NULL;

-- 구매로 판매완료된 상품을 되돌리기 위해 대상을 먼저 확보한다.
CREATE TEMP TABLE consumed_product_ids AS
SELECT DISTINCT product_id
FROM purchase
WHERE number NOT LIKE 'PC-BULK-%'
  AND buyer_id IN (SELECT member_id FROM loadtest_actor_ids);

-- 정산 → 구매
DELETE FROM settlement
WHERE purchase_id IN (
    SELECT id FROM purchase
    WHERE number NOT LIKE 'PC-BULK-%'
      AND buyer_id IN (SELECT member_id FROM loadtest_actor_ids)
);

DELETE FROM purchase
WHERE number NOT LIKE 'PC-BULK-%'
  AND buyer_id IN (SELECT member_id FROM loadtest_actor_ids);

-- 판매완료된 배경 상품을 다시 판매중으로 되돌린다.
-- 이걸 하지 않으면 다음 사람이 구매 테스트를 할 상품이 줄어든다.
UPDATE product
SET status = 'ON_SALE', updated_at = NOW()
WHERE id IN (SELECT product_id FROM consumed_product_ids)
  AND status = 'SOLD_OUT'
  AND model_number LIKE 'BULK-%';

-- 결제 (pg_payment 가 payment 를 참조하므로 자식부터 지운다)
DELETE FROM pg_payment
WHERE payment_id IN (
    SELECT id FROM payment
    WHERE member_id IN (SELECT member_id FROM loadtest_actor_ids)
);

DELETE FROM payment
WHERE member_id IN (SELECT member_id FROM loadtest_actor_ids);

-- 구매용 계정의 지갑 로그를 정리하고 잔액을 시딩 상태로 되돌린다.
DELETE FROM wallet_log
WHERE wallet_id IN (
    SELECT w.id FROM wallet w
    JOIN auth_account a ON a.member_id = w.member_id
    WHERE a.email LIKE 'loadtest-buyer-%'
);

UPDATE wallet
SET balance = 100000000.00, held_balance = 0.00, updated_at = NOW()
WHERE member_id IN (
    SELECT member_id FROM auth_account WHERE email LIKE 'loadtest-buyer-%'
);

DROP TABLE consumed_product_ids;

-- 1. 부하테스트 계정이 만든 오퍼 → 스냅샷
--    고정 계정(loadtest-buyer-N)도 오퍼를 만들므로 actor 기준으로 지운다.
--    runtime_member_ids는 고정 계정을 제외하기 때문에 여기 쓰면 안 된다.
--    (남겨두면 스냅샷이 offer_id를 물고 있어 재실행 시 OFS-001로 전부 실패한다)
DELETE FROM offer
WHERE buyer_id IN (SELECT member_id FROM loadtest_actor_ids)
   OR seller_id IN (SELECT member_id FROM loadtest_actor_ids);

DELETE FROM offer_snapshot
WHERE writer_id IN (SELECT member_id FROM loadtest_actor_ids)
   OR seller_id IN (SELECT member_id FROM loadtest_actor_ids);

-- 2. 스크랩 / 장바구니
--    오퍼와 마찬가지로 고정 계정(loadtest-buyer-N)이 만드는 데이터다.
--    scrap은 (member_id, product_id) 중복을 막고 있어(ScrapErrorCode.DUPLICATE_SCRAP)
--    남겨두면 재실행 시 스크랩 추가가 전부 실패한다.
DELETE FROM scrap WHERE member_id IN (SELECT member_id FROM loadtest_actor_ids);

DELETE FROM cart_item
WHERE cart_id IN (
    SELECT id FROM cart WHERE member_id IN (SELECT member_id FROM loadtest_actor_ids)
);
DELETE FROM cart WHERE member_id IN (SELECT member_id FROM loadtest_actor_ids);

DROP TABLE loadtest_actor_ids;

-- 3. 런타임에 만들어진 상품 (product.js MODE=write, offer 시나리오 타깃)
--    이들에 딸린 검색행/스토리/이미지도 함께 지운다.
DELETE FROM search_product
WHERE model_number LIKE 'LT-%' OR model_number LIKE 'OFFER-TARGET-%';

DELETE FROM story
WHERE product_image_id IN (
    SELECT pi.id FROM product_image pi
    JOIN product p ON p.id = pi.product_id
    WHERE p.model_number LIKE 'LT-%' OR p.model_number LIKE 'OFFER-TARGET-%'
);

DELETE FROM product_image
WHERE product_id IN (
    SELECT id FROM product
    WHERE model_number LIKE 'LT-%' OR model_number LIKE 'OFFER-TARGET-%'
);

DELETE FROM product
WHERE model_number LIKE 'LT-%' OR model_number LIKE 'OFFER-TARGET-%';

-- 4. 지갑 → 판매자 → 인증계정 → 회원
DELETE FROM wallet_log
WHERE wallet_id IN (
    SELECT id FROM wallet WHERE member_id IN (SELECT member_id FROM runtime_member_ids)
);
DELETE FROM wallet WHERE member_id IN (SELECT member_id FROM runtime_member_ids);

DELETE FROM seller WHERE member_id IN (SELECT member_id FROM runtime_member_ids);

DELETE FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
  AND email NOT LIKE 'loadtest-buyer-%';

DELETE FROM member WHERE id IN (SELECT member_id FROM runtime_member_ids);

DROP TABLE runtime_member_ids;

COMMIT;

-- 대량 삭제 후 통계 갱신
ANALYZE member;
ANALYZE auth_account;
ANALYZE product;
ANALYZE offer;

\echo ''
\echo '=== 정리 후 기준선 ==='
SELECT 'member 총계' AS metric, COUNT(*) FROM member
UNION ALL SELECT 'product(ON_SALE)', COUNT(*) FROM product WHERE status = 'ON_SALE'
UNION ALL SELECT 'offer 총계', COUNT(*) FROM offer
UNION ALL SELECT 'search_product', COUNT(*) FROM search_product
UNION ALL SELECT 'purchase 총계', COUNT(*) FROM purchase
UNION ALL SELECT 'payment 총계', COUNT(*) FROM payment;
