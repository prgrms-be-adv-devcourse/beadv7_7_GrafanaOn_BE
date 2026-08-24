-- ===========================================================================
-- 부하테스트용 대량 데이터 시딩
--
-- 왜 필요한가:
--   상품 200개짜리 테이블은 PostgreSQL이 통째로 메모리에 올려 seq scan으로 긁는다.
--   인덱스를 타는지, 쿼리 플랜이 어떤지가 전혀 드러나지 않아 "전부 p95 20ms" 같은
--   무의미한 결과가 나온다. 실서비스 1년 치 정도의 데이터를 미리 넣어야
--   LIKE 검색·N+1·기간 집계 같은 실제 병목이 보인다.
--
-- 무엇을 넣는가:
--   앱 로직이 필요 없는 "배경 데이터"만 SQL로 대량 생성한다.
--   실제로 로그인해서 요청을 보내는 테스트 계정은 k6 scenarios/seed.js가 만든다.
--   (이 SQL로 만든 회원은 로그인하지 않는다 — 비밀번호 해시가 더미다)
--
-- 실행:
--   psql -h <DB_HOST> -U <USER> -d dear -f sql/seed-bulk.sql
--
--   변수를 바꾸려면 -v 옵션을 쓴다 (아래 기본값을 덮어쓴다):
--   psql -h <HOST> -U <USER> -d dear \
--        -v member_count=500 -v product_count=10000 \
--        -f sql/seed-bulk.sql
--
-- ⚠️ 운영 DB에서는 절대 실행하지 말 것.
-- ⚠️ 정리는 sql/cleanup-bulk.sql 로 한다.
-- ===========================================================================

-- ── 변수 (psql -v 로 덮어쓸 수 있다) ──────────────────────────────────
-- 이미 -v로 넘어온 값이 있으면 그대로 두고, 없을 때만 기본값을 세팅한다.
-- psql 메타명령은 줄 단위로 처리되므로 한 줄에 붙여 쓰지 않는다.

\if :{?member_count}
\else
\set member_count 1000
\endif

\if :{?seller_ratio}
\else
\set seller_ratio 10
\endif

\if :{?product_count}
\else
\set product_count 30000
\endif

\if :{?images_per_product}
\else
\set images_per_product 3
\endif

\if :{?offer_count}
\else
\set offer_count 10000
\endif

\if :{?hot_product_ratio}
\else
\set hot_product_ratio 10
\endif

\if :{?purchase_count}
\else
\set purchase_count 5000
\endif

\if :{?settlement_months}
\else
\set settlement_months 12
\endif

\if :{?cart_items_per_member}
\else
\set cart_items_per_member 15
\endif

\echo '=== 시딩 설정 ==='
\echo '회원 수              :' :member_count
\echo '판매자 비율(%)       :' :seller_ratio
\echo '상품 수              :' :product_count
\echo '상품당 이미지        :' :images_per_product
\echo '오퍼 수              :' :offer_count
\echo '오퍼 집중 상품 비율(%):' :hot_product_ratio
\echo '구매 이력 수         :' :purchase_count
\echo '정산 이력 개월 수    :' :settlement_months
\echo '회원당 장바구니 항목 :' :cart_items_per_member
\echo ''

\timing on

BEGIN;

-- ── 1. 회원 ──────────────────────────────────────────────────────────
-- nickname에 unique 제약이 있으므로 순번으로 유일성을 보장한다.
-- 식별 접두사 'bulk_' 로 나중에 정리할 수 있게 한다.
\echo '[1/9] member 생성...'
INSERT INTO member (name, default_shipping_address, phone_number, nickname, status, inserted_at, updated_at)
SELECT
    '벌크회원' || i,
    '서울특별시 강남구 테헤란로 ' || (i % 500 + 1),
    '010-' || LPAD(((i % 9000) + 1000)::text, 4, '0') || '-' || LPAD(((i * 7 % 9000) + 1000)::text, 4, '0'),
    'bulk_user_' || i,
    'ACTIVE',
    NOW() - (i % 365 || ' days')::interval,
    NOW()
FROM generate_series(1, :member_count) AS i;

-- ── 2. 인증 계정 ─────────────────────────────────────────────────────
-- email 인덱스에 데이터 볼륨을 주는 것이 목적이다.
-- password_hash는 더미라 이 계정들로는 로그인할 수 없다(의도된 동작).
\echo '[2/9] auth_account 생성...'
INSERT INTO auth_account (member_id, email, password_hash, status, role, inserted_at, updated_at)
SELECT
    m.id,
    'bulk-' || m.id || '@example.com',
    '$2a$10$DUMMYHASHFORLOADTESTONLYxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    'ACTIVE',
    -- 판매자로 뽑힐 회원은 SELLER, 나머지는 BUYER (3단계와 동일한 조건)
    CASE
        WHEN (REGEXP_REPLACE(m.nickname, '\D', '', 'g'))::bigint % 100 < :seller_ratio
        THEN 'SELLER'
        ELSE 'BUYER'
    END,
    m.inserted_at,
    NOW()
FROM member m
WHERE m.nickname LIKE 'bulk\_user\_%';

-- ── 3. 판매자 ────────────────────────────────────────────────────────
-- 전체 회원 중 seller_ratio(%) 만큼만 판매자로 만든다.
\echo '[3/9] seller 생성...'
-- registered_at은 Seller 엔티티에 NOT NULL로 추가된 컬럼이다.
-- 실제 판매자 등록 시점 의미이므로 가입 시점(inserted_at)으로 채운다.
INSERT INTO seller (member_id, bank, account, status, registered_at, inserted_at, updated_at)
SELECT
    m.id,
    (ARRAY['국민은행','신한은행','우리은행','하나은행','카카오뱅크'])[(m.id % 5) + 1],
    LPAD((m.id * 12345 % 1000000)::text, 6, '0') || '-01-' || LPAD((m.id % 1000000)::text, 6, '0'),
    'ACTIVE',
    m.inserted_at,
    m.inserted_at,
    NOW()
FROM member m
WHERE m.nickname LIKE 'bulk\_user\_%'
  AND (REGEXP_REPLACE(m.nickname, '\D', '', 'g'))::bigint % 100 < :seller_ratio;

-- 방금 만든 bulk 판매자만 따로 담아둔다. 상품 배분에 이 목록만 쓴다.
--
-- ⚠️ seller 테이블 전체에서 고르면 안 된다.
--    부하테스트로 생성된 loadtest-% 계정도 판매자로 등록되어 seller 테이블에 남아 있다.
--    그 계정이 BULK 상품의 소유자가 되면, cleanup-runtime.sql이 그 계정을 지울 때
--    상품은 BULK-%라 남고 소유자만 사라져서 데이터가 깨진다.
--    (실제로 2026-08-09에 상품 2,607개가 고아가 되고 BULK 오퍼 880건이 함께 삭제됐다)
CREATE TEMP TABLE bulk_sellers AS
SELECT
    s.member_id,
    ROW_NUMBER() OVER (ORDER BY s.member_id) - 1 AS idx
FROM seller s
JOIN member m ON m.id = s.member_id
WHERE m.nickname LIKE 'bulk\_user\_%';

-- ── 4. 상품 ──────────────────────────────────────────────────────────
-- model_number를 'BULK-{i}' 로 두어 이후 조인 키와 정리 기준으로 쓴다.
-- 검색 테스트가 의미 있으려면 이름에 실제 검색어가 섞여 있어야 한다.
\echo '[4/9] product 생성...'
INSERT INTO product (
    seller_id, name, brand, model_number, category, release_date,
    price, sale_type, status, view_count, description, inserted_at, updated_at
)
SELECT
    sellers.member_id,
    (ARRAY['나이키','아디다스','신발','가방','자켓'])[(i % 5) + 1] || ' 벌크상품 ' || i,
    (ARRAY['나이키','아디다스','뉴발란스','컨버스','반스'])[(i % 5) + 1],
    'BULK-' || i,
    (ARRAY['SNEAKERS','SPORTS_SHOES','DRESS_SHOES','BOOTS','SANDALS_SLIDES','WINTER_SHOES'])[(i % 6) + 1],
    DATE '2024-01-01' + (i % 700),
    (50000 + (i % 30) * 10000)::numeric(15,2),
    CASE WHEN i % 2 = 0 THEN 'IMMEDIATE' ELSE 'OFFER' END,
    'ON_SALE',
    (i * 13 % 5000),
    '부하테스트 벌크 상품 ' || i || ' 설명입니다.',
    NOW() - (i % 365 || ' days')::interval,
    NOW()
FROM generate_series(1, :product_count) AS i
CROSS JOIN LATERAL (
    -- 상품을 bulk 판매자들에게만 고르게 분배한다 (위 bulk_sellers 주석 참고).
    SELECT member_id
    FROM bulk_sellers
    WHERE idx = i % GREATEST((SELECT COUNT(*) FROM bulk_sellers), 1)
    LIMIT 1
) AS sellers;

-- ── 5. 상품 이미지 + 스토리 ──────────────────────────────────────────
-- 상세 조회의 N+1 여부를 드러내려면 상품당 이미지가 여러 개여야 한다.
\echo '[5/9] product_image / story 생성...'
INSERT INTO product_image (product_id, url, sort_order, inserted_at, updated_at)
SELECT p.id, 'https://example.com/bulk/' || p.id || '-' || j || '.jpg', j, p.inserted_at, NOW()
FROM product p
CROSS JOIN generate_series(1, :images_per_product) AS j
WHERE p.model_number LIKE 'BULK-%';

-- 스토리는 STORY_CONTENT 검색 대상이므로 검색어를 섞어 넣는다.
INSERT INTO story (product_image_id, content, inserted_at, updated_at)
SELECT
    pi.id,
    (ARRAY['나이키','아디다스','신발','가방','자켓'])[(pi.id % 5) + 1]
        || ' 관련 스토리입니다. 꽁꽁 숨겨진 빈티지샵에서 발굴했습니다. 벌크 ' || pi.id,
    pi.inserted_at,
    NOW()
FROM product_image pi
JOIN product p ON p.id = pi.product_id
WHERE p.model_number LIKE 'BULK-%';

-- ── 6. 검색 테이블 ───────────────────────────────────────────────────
-- ⚠️ 중요: 검색 API는 product가 아니라 search_product를 읽는다.
--    평소에는 상품 등록 이벤트로 채워지지만, 여기서는 SQL로 직접 넣었으므로
--    이 미러링을 하지 않으면 검색 부하테스트 결과가 전부 빈 결과가 된다.
\echo '[6/9] search_product 미러링...'
INSERT INTO search_product (
    id, name, model_number, category, release_date, price, sale_type,
    view_count, description, story_content, product_inserted_at, inserted_at, updated_at
)
SELECT
    p.id, p.name, p.model_number, p.category, p.release_date, p.price, p.sale_type,
    p.view_count, p.description,
    COALESCE(
        (SELECT STRING_AGG(s.content, ' ')
         FROM product_image pi
         JOIN story s ON s.product_image_id = pi.id
         WHERE pi.product_id = p.id),
        ''
    ),
    p.inserted_at, p.inserted_at, NOW()
FROM product p
WHERE p.model_number LIKE 'BULK-%'
ON CONFLICT (id) DO NOTHING;

-- ── 7. 장바구니 ──────────────────────────────────────────────────────
-- cart_item에 (cart_id, product_id) 유니크 제약이 있으므로
-- 한 장바구니 안에서 상품이 겹치지 않도록 계산한다.
\echo '[7/9] cart / cart_item 생성...'
INSERT INTO cart (member_id, inserted_at, updated_at)
SELECT m.id, m.inserted_at, NOW()
FROM member m
WHERE m.nickname LIKE 'bulk\_user\_%';

-- cart_item_status는 smallint다.
-- CartItem 엔티티에 @Enumerated(EnumType.STRING)이 없어 JPA 기본값인 ORDINAL로 저장된다.
--   CartItemStatus: BEFORE_PAYMENT=0, PAYMENT_COMPLETED=1
-- 다른 엔티티의 enum은 전부 STRING이라 문자열로 넣지만, 이것만 예외다.
INSERT INTO cart_item (cart_id, product_id, cart_item_status, inserted_at, updated_at)
SELECT c.id, p.id, 0, NOW(), NOW()
FROM cart c
JOIN member m ON m.id = c.member_id AND m.nickname LIKE 'bulk\_user\_%'
CROSS JOIN generate_series(1, :cart_items_per_member) AS j
JOIN LATERAL (
    SELECT id FROM product
    WHERE model_number = 'BULK-' || (((c.id * 37 + j) % :product_count) + 1)
    LIMIT 1
) p ON TRUE
ON CONFLICT ON CONSTRAINT uk_cart_item_cart_product DO NOTHING;

-- ── 8. 오퍼 (스냅샷 → 오퍼) ──────────────────────────────────────────
-- 실제 서비스처럼 일부 인기 상품에 오퍼가 몰리도록 편중시킨다.
-- (hot_product_ratio % 의 상품에 전체 오퍼가 집중된다)
\echo '[8/9] offer_snapshot / offer 생성...'
CREATE TEMP TABLE bulk_offer_seed AS
SELECT
    i AS seq,
    p.id            AS product_id,
    p.seller_id     AS seller_id,
    p.model_number  AS model_number,
    p.price         AS price,
    b.id            AS buyer_id
FROM generate_series(1, :offer_count) AS i
JOIN LATERAL (
    -- 오퍼는 OFFER 타입 상품에만 달린다. 상위 hot_product_ratio% 안에서 고른다.
    SELECT id, seller_id, model_number, price
    FROM product
    WHERE model_number = 'BULK-' || (
        ((i % GREATEST((:product_count * :hot_product_ratio / 100), 1)) * 2) + 1
    )
    LIMIT 1
) p ON TRUE
JOIN LATERAL (
    -- 구매자는 판매자 본인이 아니어야 한다.
    SELECT m.id FROM member m
    WHERE m.nickname = 'bulk_user_' || ((i % :member_count) + 1)
      AND m.id <> p.seller_id
    LIMIT 1
) b ON TRUE;

INSERT INTO offer_snapshot (
    seller_id, writer_id, product_id, model_number_snapshot, price_snapshot,
    inserted_at, updated_at
)
SELECT seller_id, buyer_id, product_id, model_number, price, NOW(), NOW()
FROM bulk_offer_seed;

-- version은 Offer 엔티티의 낙관적 락(@Version) 필드다. NOT NULL이라 채워야 하고,
-- SQL로 직접 넣는 데이터라 락 경합이 없으므로 초기값 0이면 된다.
INSERT INTO offer (
    number, version, buyer_id, seller_id, product_id, snapshot_id, amount,
    title, story, delivery, status, payment_status, inserted_at, updated_at
)
SELECT
    'OF-BULK-' || s.seq,
    0,
    s.buyer_id, s.seller_id, s.product_id, snap.id, s.price,
    '벌크 오퍼 ' || s.seq,
    '부하테스트용 벌크 오퍼 스토리입니다.',
    '서울특별시 강남구 테헤란로 123',
    'PENDING',
    'PAYMENT_PENDING',
    NOW() - (s.seq % 365 || ' days')::interval,
    NOW()
FROM bulk_offer_seed s
JOIN LATERAL (
    SELECT id FROM offer_snapshot
    WHERE product_id = s.product_id AND writer_id = s.buyer_id
    ORDER BY id DESC LIMIT 1
) snap ON TRUE;

-- ── 9. 구매 이력 + 정산 ──────────────────────────────────────────────
-- 구매된 상품은 SOLD_OUT으로 바꿔 실제 상태와 일관성을 맞춘다.
\echo '[9/9] purchase / settlement 생성...'
CREATE TEMP TABLE bulk_purchase_seed AS
SELECT
    i AS seq,
    p.id        AS product_id,
    p.seller_id AS seller_id,
    p.price     AS price,
    b.id        AS buyer_id,
    -- 정산 이력 조회를 위해 settlement_months 개월에 걸쳐 분산시킨다.
    NOW() - ((i % (:settlement_months * 30)) || ' days')::interval AS purchased_at
FROM generate_series(1, :purchase_count) AS i
JOIN LATERAL (
    -- 구매는 IMMEDIATE 상품(짝수 seq)에만 발생한다.
    SELECT id, seller_id, price
    FROM product
    WHERE model_number = 'BULK-' || ((i % (:product_count / 2)) * 2 + 2)
    LIMIT 1
) p ON TRUE
JOIN LATERAL (
    SELECT m.id FROM member m
    WHERE m.nickname = 'bulk_user_' || ((i * 3 % :member_count) + 1)
      AND m.id <> p.seller_id
    LIMIT 1
) b ON TRUE;

-- version은 Purchase 엔티티의 낙관적 락(@Version) 필드다. wallet과 마찬가지로 0으로 채운다.
INSERT INTO purchase (
    number, version, buyer_id, seller_id, product_id, amount, status,
    purchased_at, payment_due_at, paid_at, delivery, inserted_at, updated_at
)
SELECT
    'PC-BULK-' || s.seq,
    0,
    s.buyer_id, s.seller_id, s.product_id, s.price,
    'PURCHASE_CONFIRMED',
    s.purchased_at,
    s.purchased_at + INTERVAL '1 day',
    s.purchased_at + INTERVAL '1 hour',
    '서울특별시 강남구 테헤란로 123',
    s.purchased_at,
    NOW()
FROM bulk_purchase_seed s;

-- 구매된 상품은 판매완료 처리
UPDATE product SET status = 'SOLD_OUT', updated_at = NOW()
WHERE id IN (SELECT product_id FROM bulk_purchase_seed);

UPDATE search_product sp SET sale_type = sale_type, updated_at = NOW()
WHERE sp.id IN (SELECT product_id FROM bulk_purchase_seed);

-- 정산 정책이 없으면 기본 정책 1건을 만든다 (수수료 5% + 고정 0원)
INSERT INTO settlement_policy (fee_rate, fixed_fee, inserted_at, updated_at)
SELECT 0.0500, 0.00, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM settlement_policy);

-- 판매자 지갑이 없으면 만든다 (정산은 wallet_id를 참조한다)
INSERT INTO wallet (member_id, balance, held_balance, version, inserted_at, updated_at)
SELECT DISTINCT s.member_id, 0.00, 0.00, 0, NOW(), NOW()
FROM seller s
WHERE NOT EXISTS (SELECT 1 FROM wallet w WHERE w.member_id = s.member_id)
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO settlement (
    settlement_policy_id, wallet_id, purchase_id, gross_amount, fee_amount, net_amount,
    state, inserted_at, updated_at
)
SELECT
    (SELECT id FROM settlement_policy ORDER BY id LIMIT 1),
    w.id,
    pc.id,
    pc.amount,
    ROUND(pc.amount * 0.05, 2),
    pc.amount - ROUND(pc.amount * 0.05, 2),
    'COMPLETED',
    pc.purchased_at,
    NOW()
FROM purchase pc
JOIN wallet w ON w.member_id = pc.seller_id
WHERE pc.number LIKE 'PC-BULK-%';

DROP TABLE bulk_sellers;
DROP TABLE bulk_offer_seed;
DROP TABLE bulk_purchase_seed;

COMMIT;

-- 대량 INSERT 후에는 통계를 갱신해야 쿼리 플래너가 제대로 동작한다.
-- (이걸 빼먹으면 플래너가 옛 통계로 잘못된 플랜을 고른다)
\echo 'ANALYZE 실행 중...'
ANALYZE member;
ANALYZE auth_account;
ANALYZE seller;
ANALYZE product;
ANALYZE product_image;
ANALYZE story;
ANALYZE search_product;
ANALYZE cart;
ANALYZE cart_item;
ANALYZE offer;
ANALYZE offer_snapshot;
ANALYZE purchase;
ANALYZE settlement;

\timing off

-- ── 결과 확인 ────────────────────────────────────────────────────────
\echo ''
\echo '=== 시딩 결과 ==='
SELECT 'member'         AS table_name, COUNT(*) FROM member         WHERE nickname LIKE 'bulk\_user\_%'
UNION ALL SELECT 'seller',        COUNT(*) FROM seller s JOIN member m ON m.id = s.member_id WHERE m.nickname LIKE 'bulk\_user\_%'
UNION ALL SELECT 'product',       COUNT(*) FROM product        WHERE model_number LIKE 'BULK-%'
UNION ALL SELECT 'product_image', COUNT(*) FROM product_image pi JOIN product p ON p.id = pi.product_id WHERE p.model_number LIKE 'BULK-%'
UNION ALL SELECT 'search_product',COUNT(*) FROM search_product WHERE model_number LIKE 'BULK-%'
UNION ALL SELECT 'cart_item',     COUNT(*) FROM cart_item ci JOIN cart c ON c.id = ci.cart_id JOIN member m ON m.id = c.member_id WHERE m.nickname LIKE 'bulk\_user\_%'
UNION ALL SELECT 'offer',         COUNT(*) FROM offer          WHERE number LIKE 'OF-BULK-%'
UNION ALL SELECT 'purchase',      COUNT(*) FROM purchase       WHERE number LIKE 'PC-BULK-%'
UNION ALL SELECT 'settlement',    COUNT(*) FROM settlement st JOIN purchase pc ON pc.id = st.purchase_id WHERE pc.number LIKE 'PC-BULK-%';

-- ── 정합성 확인 ──────────────────────────────────────────────────────
-- 모든 BULK 상품의 소유자가 bulk 판매자여야 한다.
-- 0이 아니면 상품 배분이 잘못된 것이므로 시딩을 다시 해야 한다.
\echo ''
\echo '=== 정합성 확인 (전부 0이어야 정상) ==='
SELECT
    (SELECT COUNT(*) FROM product p
     WHERE p.model_number LIKE 'BULK-%'
       AND NOT EXISTS (SELECT 1 FROM member m WHERE m.id = p.seller_id)
    ) AS "소유자 없는 상품",
    (SELECT COUNT(*) FROM product p
     JOIN member m ON m.id = p.seller_id
     WHERE p.model_number LIKE 'BULK-%'
       AND m.nickname NOT LIKE 'bulk\_user\_%'
    ) AS "bulk 아닌 판매자 소유 상품";
