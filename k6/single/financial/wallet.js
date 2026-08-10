import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/deposits/me
// 지갑 잔액 조회. 단건 조회라 가볍지만 기준선 확보용으로 측정한다.

export const options = singleOptions('GET /api/deposits/me');

export default function () {
    const session = getSession();

    const res = http.get(`${COMMERCE_BASE_URL}/api/deposits/me`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/deposits/me' },
    });

    check(res, { '지갑 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
