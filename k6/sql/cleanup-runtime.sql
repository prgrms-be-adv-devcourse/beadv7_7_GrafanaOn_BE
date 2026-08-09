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

-- 1. 이 계정들이 만든 오퍼 → 스냅샷
DELETE FROM offer
WHERE buyer_id IN (SELECT member_id FROM runtime_member_ids)
   OR seller_id IN (SELECT member_id FROM runtime_member_ids);

DELETE FROM offer_snapshot
WHERE writer_id IN (SELECT member_id FROM runtime_member_ids)
   OR seller_id IN (SELECT member_id FROM runtime_member_ids);

-- 2. 스크랩 / 장바구니
DELETE FROM scrap WHERE member_id IN (SELECT member_id FROM runtime_member_ids);

DELETE FROM cart_item
WHERE cart_id IN (
    SELECT id FROM cart WHERE member_id IN (SELECT member_id FROM runtime_member_ids)
);
DELETE FROM cart WHERE member_id IN (SELECT member_id FROM runtime_member_ids);

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
UNION ALL SELECT 'search_product', COUNT(*) FROM search_product;
