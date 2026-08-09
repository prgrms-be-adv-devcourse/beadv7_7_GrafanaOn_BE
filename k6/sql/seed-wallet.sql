-- 부하테스트용 지갑 잔액 시딩
--
-- 왜 필요한가:
--   구매(POST /api/purchases)는 지갑 잔액을 차감하는데, 잔액을 채우는 API가 없다.
--     - POST /internal/deposits    : 지갑 "생성"만 하고 잔액은 0
--     - POST /api/payments/confirm : 실제 토스 API 호출 → 부하테스트에서 사용 금지
--   따라서 DB에 직접 넣는다.
--
-- 실행 순서:
--   1) k6 scenarios/seed.js 실행 → loadtest-buyer-N@example.com 계정 생성
--   2) 이 SQL 실행                → 해당 계정들의 지갑 생성 + 잔액 충전
--   3) k6 scenarios/purchase.js -e MODE=write 실행
--
-- 전제:
--   identity(auth_account)와 commerce(wallet)가 같은 DB(dear)를 쓰는 현재 구조를 전제한다.
--   서비스별 DB가 분리되면 아래 조인 대신 member_id를 직접 나열하는 방식으로 바꿔야 한다.
--
-- ⚠️ 운영 DB에서는 절대 실행하지 말 것.

BEGIN;

-- 지갑 생성 + 잔액 충전 (이미 있으면 잔액만 갱신)
INSERT INTO wallet (member_id, balance, held_balance, version, inserted_at, updated_at)
SELECT
    a.member_id,
    100000000.00,   -- 1억. 반복 구매로도 소진되지 않을 금액
    0.00,
    0,
    NOW(),
    NOW()
FROM auth_account a
WHERE a.email LIKE 'loadtest-buyer-%@example.com'
  AND a.member_id IS NOT NULL
ON CONFLICT (member_id) DO UPDATE
SET balance      = EXCLUDED.balance,
    held_balance = 0.00,
    updated_at   = NOW();

COMMIT;

-- 결과 확인
SELECT a.email,
       w.member_id,
       w.balance,
       w.held_balance
FROM wallet w
JOIN auth_account a ON a.member_id = w.member_id
WHERE a.email LIKE 'loadtest-buyer-%@example.com'
ORDER BY w.member_id;

-- 참고: wallet_log에는 TOPUP 기록이 남지 않는다(SQL로 직접 넣었으므로).
-- 잔액과 로그가 불일치하므로, 정산/이력 조회 결과까지 함께 검증하려면
-- 아래처럼 로그도 같이 넣어야 한다. (reference_id는 아무 값이나 무방)
--
-- INSERT INTO wallet_log (wallet_id, type, amount, reference_id, inserted_at, updated_at)
-- SELECT w.id, 'TOPUP', 100000000.00, 0, NOW(), NOW()
-- FROM wallet w
-- JOIN auth_account a ON a.member_id = w.member_id
-- WHERE a.email LIKE 'loadtest-buyer-%@example.com';
