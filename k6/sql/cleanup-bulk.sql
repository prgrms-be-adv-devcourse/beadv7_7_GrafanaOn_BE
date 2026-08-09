-- ===========================================================================
-- 대량 시딩 데이터 정리 (sql/seed-bulk.sql로 넣은 것만)
--
-- 실행:
--   psql -h <DB_HOST> -U <USER> -d dear -f sql/cleanup-bulk.sql
--
-- 식별 규칙 (seed-bulk.sql이 넣은 접두사)
--   member.nickname      LIKE 'bulk_user_%'
--   product.model_number LIKE 'BULK-%'
--   offer.number         LIKE 'OF-BULK-%'
--   purchase.number      LIKE 'PC-BULK-%'
--
-- ⚠️ 운영 DB에서는 절대 실행하지 말 것.
-- ⚠️ k6 seed.js가 만든 테스트 계정(loadtest-%)은 sql/cleanup.sql로 따로 정리한다.
-- ===========================================================================

-- ── 삭제 대상 확인 (먼저 이것부터) ───────────────────────────────────
\echo '=== 삭제 대상 ==='
SELECT 'member'          AS table_name, COUNT(*) FROM member   WHERE nickname LIKE 'bulk\_user\_%'
UNION ALL SELECT 'product',  COUNT(*) FROM product  WHERE model_number LIKE 'BULK-%'
UNION ALL SELECT 'offer',    COUNT(*) FROM offer    WHERE number LIKE 'OF-BULK-%'
UNION ALL SELECT 'purchase', COUNT(*) FROM purchase WHERE number LIKE 'PC-BULK-%';

BEGIN;

-- 참조하는 쪽부터 역순으로 지운다.

-- 1. 정산
DELETE FROM settlement
WHERE purchase_id IN (SELECT id FROM purchase WHERE number LIKE 'PC-BULK-%');

-- 2. 구매
DELETE FROM purchase WHERE number LIKE 'PC-BULK-%';

-- 3. 오퍼 → 스냅샷
DELETE FROM offer WHERE number LIKE 'OF-BULK-%';

DELETE FROM offer_snapshot
WHERE product_id IN (SELECT id FROM product WHERE model_number LIKE 'BULK-%');

-- 4. 장바구니
DELETE FROM cart_item
WHERE product_id IN (SELECT id FROM product WHERE model_number LIKE 'BULK-%');

DELETE FROM cart
WHERE member_id IN (SELECT id FROM member WHERE nickname LIKE 'bulk\_user\_%');

-- 5. 검색 테이블 → 스토리 → 이미지 → 상품
DELETE FROM search_product WHERE model_number LIKE 'BULK-%';

DELETE FROM story
WHERE product_image_id IN (
    SELECT pi.id FROM product_image pi
    JOIN product p ON p.id = pi.product_id
    WHERE p.model_number LIKE 'BULK-%'
);

DELETE FROM product_image
WHERE product_id IN (SELECT id FROM product WHERE model_number LIKE 'BULK-%');

DELETE FROM product WHERE model_number LIKE 'BULK-%';

-- 6. 지갑 → 판매자 → 인증계정 → 회원
CREATE TEMP TABLE bulk_member_ids AS
SELECT id FROM member WHERE nickname LIKE 'bulk\_user\_%';

DELETE FROM wallet_log
WHERE wallet_id IN (
    SELECT id FROM wallet WHERE member_id IN (SELECT id FROM bulk_member_ids)
);

DELETE FROM wallet WHERE member_id IN (SELECT id FROM bulk_member_ids);

DELETE FROM seller WHERE member_id IN (SELECT id FROM bulk_member_ids);

DELETE FROM auth_account WHERE member_id IN (SELECT id FROM bulk_member_ids);

DELETE FROM member WHERE id IN (SELECT id FROM bulk_member_ids);

DROP TABLE bulk_member_ids;

COMMIT;

ANALYZE;

\echo '=== 정리 완료 ==='
