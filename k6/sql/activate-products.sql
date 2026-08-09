-- ===========================================================================
-- 부하테스트용 상품 판매중(ON_SALE) 전환
--
-- 왜 필요한가:
--   POST /api/products로 생성한 상품은 status=PREPARING으로 시작한다.
--   그런데 이를 ON_SALE로 바꾸는 API가 없다.
--   (Product.changeStatusToOnSale()이 테스트 코드에서만 호출되고
--    프로덕션 코드 어디에서도 호출되지 않는다)
--
--   PREPARING 상품은 오퍼·구매 대상이 될 수 없다.
--     POST /api/offers/snapshot  → OF-008 상품 정보를 조회하지 못했습니다
--     POST /api/purchases        → 동일하게 실패
--
--   따라서 k6 scenarios/seed.js로 만든 상품을 오퍼/구매 테스트에 쓰려면
--   이 SQL로 상태를 직접 바꿔야 한다.
--   (sql/seed-bulk.sql로 만든 상품은 처음부터 ON_SALE이라 불필요)
--
-- 실행:
--   psql -h <DB_HOST> -U <USER> -d dear -f sql/activate-products.sql
--
-- ⚠️ 운영 DB에서는 절대 실행하지 말 것.
-- ===========================================================================

-- ── 대상 확인 ────────────────────────────────────────────────────────
\echo '=== 전환 대상 (PREPARING 상태의 부하테스트 상품) ==='
SELECT model_number, status, COUNT(*) OVER () AS total
FROM product
WHERE status = 'PREPARING'
  AND (model_number LIKE 'SEED-%' OR model_number LIKE 'LT-%' OR model_number LIKE 'OFFER-TARGET-%')
ORDER BY id
LIMIT 10;

BEGIN;

UPDATE product
SET status = 'ON_SALE',
    updated_at = NOW()
WHERE status = 'PREPARING'
  AND (
       model_number LIKE 'SEED-%'          -- k6 seed.js가 만든 상품
    OR model_number LIKE 'LT-%'            -- product.js MODE=write가 만든 상품
    OR model_number LIKE 'OFFER-TARGET-%'  -- offer 시나리오용
  );

COMMIT;

-- ── 결과 확인 ────────────────────────────────────────────────────────
\echo ''
\echo '=== 전환 후 상태별 건수 ==='
SELECT status, sale_type, COUNT(*)
FROM product
WHERE model_number LIKE 'SEED-%'
   OR model_number LIKE 'LT-%'
   OR model_number LIKE 'BULK-%'
GROUP BY status, sale_type
ORDER BY status, sale_type;
