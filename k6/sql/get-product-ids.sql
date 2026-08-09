-- ===========================================================================
-- 시나리오에 넘길 PRODUCT_IDS 값을 뽑는다
--
-- 왜 필요한가:
--   각 시나리오의 setup()은 PRODUCT_IDS가 없으면 GET /api/products를 호출해
--   상품 ID를 확보한다. 그런데 이 API는 페이지네이션이 없어서
--   ON_SALE 전체 조회 시 4.5MB / 29초가 걸린다(CAUTION 참고).
--   테스트를 돌릴 때마다 30초를 버리게 되므로, 미리 ID를 뽑아 -e 로 넘기는 편이 낫다.
--
-- 실행:
--   docker exec -i postgres psql -U dev2 -d dear < sql/get-product-ids.sql
--
-- 출력된 값을 그대로 -e PRODUCT_IDS=... 로 넘긴다.
-- ===========================================================================

\pset tuples_only on
\pset format unaligned

\echo ''
\echo '=== [조회 계열: product / cart / scrap] ON_SALE 상품 200개 ==='
SELECT string_agg(id::text, ',')
FROM (
    SELECT id FROM product
    WHERE status = 'ON_SALE'
    ORDER BY id
    LIMIT 200
) t;

\echo ''
\echo '=== [offer.js] ON_SALE + OFFER 상품 500개 ==='
\echo '(오퍼는 한 구매자가 한 상품에 1회만 가능하므로 넉넉히 뽑는다)'
SELECT string_agg(id::text, ',')
FROM (
    SELECT id FROM product
    WHERE status = 'ON_SALE' AND sale_type = 'OFFER'
    ORDER BY id
    LIMIT 500
) t;

\echo ''
\echo '=== [purchase.js MODE=write] ON_SALE + IMMEDIATE 상품 200개 ==='
\echo '(구매 1건당 상품 1개가 소모된다. burst 기준 약 90건 필요)'
SELECT string_agg(id::text, ',')
FROM (
    SELECT id FROM product
    WHERE status = 'ON_SALE' AND sale_type = 'IMMEDIATE'
    ORDER BY id
    LIMIT 200
) t;

\pset tuples_only off
\pset format aligned
