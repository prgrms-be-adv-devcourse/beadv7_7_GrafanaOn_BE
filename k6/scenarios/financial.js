import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import { getSession, authHeaders } from '../config/auth.js';

// Financial 시나리오: 지갑 잔액 + 정산 예정금액 + 정산 이력 (모두 조회)
//
// ⚠️ 결제 쓰기 API는 의도적으로 제외했다.
//  - POST /api/payments/confirm : TossPaymentApprovalAdapter를 통해 실제 토스 API를
//    호출한다. 부하테스트로 외부 PG에 트래픽을 보내면 안 되므로 절대 포함하지 말 것.
//  - POST /api/payments/charge  : 결제 레코드를 생성한다. 외부 호출 여부와 무관하게
//    반복 실행 시 미완료 결제가 계속 쌓이므로 제외했다.
// 결제 경로 성능이 필요하면 PG를 모킹한 환경에서 별도 스크립트로 측정할 것.

export const options = { stages, thresholds: THRESHOLDS };

function currentYearMonth() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function dateRange() {
    const end = new Date();
    const start = new Date();
    start.setMonth(start.getMonth() - 1);

    const fmt = (d) => d.toISOString().slice(0, 10);
    return { startDate: fmt(start), endDate: fmt(end) };
}

export default function () {
    const session = getSession();

    // 1. 내 지갑 잔액 조회
    const walletRes = http.get(`${COMMERCE_BASE_URL}/api/deposits/me`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/deposits/me' },
    });
    check(walletRes, { '지갑 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 2. 정산 예정금액 조회 (특정월)
    const settlementRes = http.get(
        `${COMMERCE_BASE_URL}/api/settlements/me?targetMonth=${currentYearMonth()}`,
        {
            headers: authHeaders(session),
            tags: { name: 'GET /api/settlements/me' },
        }
    );
    check(settlementRes, { '정산 예정금액 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 3. 정산 이력 조회 (기간 조회 — 데이터 누적 시 병목 주시 대상)
    const range = dateRange();
    const historyRes = http.get(
        `${COMMERCE_BASE_URL}/api/settlements/me/history` +
        `?startDate=${range.startDate}&endDate=${range.endDate}`,
        {
            headers: authHeaders(session),
            tags: { name: 'GET /api/settlements/me/history' },
        }
    );
    check(historyRes, { '정산 이력 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
