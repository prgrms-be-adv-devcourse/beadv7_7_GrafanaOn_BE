import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/settlements/me?targetMonth=YYYY-MM
// 특정월 정산 예정금액. 집계 쿼리라 데이터가 쌓이면 무거워질 수 있다.

export const options = singleOptions('GET /api/settlements/me');

function currentYearMonth() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export default function () {
    const session = getSession();

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/settlements/me?targetMonth=${currentYearMonth()}`,
        { headers: authHeaders(session), tags: { name: 'GET /api/settlements/me' } }
    );

    check(res, { '정산 예정금액 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
