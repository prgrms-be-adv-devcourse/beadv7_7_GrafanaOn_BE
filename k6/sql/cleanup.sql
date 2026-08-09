-- 부하테스트 데이터 정리
--
-- ⚠️ 실행 전 반드시 확인할 것
--   1. 운영 DB에서는 절대 실행하지 말 것.
--   2. 아래 삭제 순서는 wallet/wallet_log 외의 FK 관계까지 전부 검증하지 않았다.
--      product를 참조하는 테이블(cart_item, offer, offer_snapshot, purchase 등)이 있으면
--      FK 위반이 발생한다. 에러가 나면 참조하는 쪽부터 지우도록 순서를 조정할 것.
--   3. 먼저 SELECT로 삭제 대상 건수를 확인한 뒤 DELETE를 실행하는 것을 권장한다.
--
-- 식별 규칙
--   부하테스트 계정 : email LIKE 'loadtest-%@example.com'
--   시드 상품       : model_number LIKE 'SEED-%'
--   런타임 생성 상품 : model_number LIKE 'LT-%'   (product.js MODE=write)

-- ── 삭제 대상 확인 (먼저 이것부터 실행) ──────────────────────────────
SELECT 'auth_account' AS target, COUNT(*) FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
UNION ALL
SELECT 'product(seed)', COUNT(*) FROM product WHERE model_number LIKE 'SEED-%'
UNION ALL
SELECT 'product(runtime)', COUNT(*) FROM product WHERE model_number LIKE 'LT-%';


-- ── 실제 정리 ────────────────────────────────────────────────────────
BEGIN;

-- 1. 지갑 로그 → 지갑
DELETE FROM wallet_log
WHERE wallet_id IN (
    SELECT w.id
    FROM wallet w
    JOIN auth_account a ON a.member_id = w.member_id
    WHERE a.email LIKE 'loadtest-%@example.com'
);

DELETE FROM wallet
WHERE member_id IN (
    SELECT member_id FROM auth_account
    WHERE email LIKE 'loadtest-%@example.com'
      AND member_id IS NOT NULL
);

-- 2. 부하테스트로 만들어진 상품
--    (참조하는 테이블이 있으면 여기서 FK 에러가 난다 → 위 주의사항 2번 참고)
DELETE FROM product WHERE model_number LIKE 'SEED-%';
DELETE FROM product WHERE model_number LIKE 'LT-%';

-- 3. 계정 → 프로필
--    member_id를 먼저 확보한 뒤 auth_account를 지운다.
CREATE TEMP TABLE loadtest_member_ids AS
SELECT member_id FROM auth_account
WHERE email LIKE 'loadtest-%@example.com'
  AND member_id IS NOT NULL;

DELETE FROM auth_account WHERE email LIKE 'loadtest-%@example.com';

DELETE FROM member WHERE id IN (SELECT member_id FROM loadtest_member_ids);

DROP TABLE loadtest_member_ids;

COMMIT;
