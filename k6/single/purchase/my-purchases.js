import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getBuyerSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/purchases/me
//
// 내 구매 목록 조회. 조회라 반복 제약이 없다.
// 고정 계정은 시딩된 구매 이력이 있어 목록이 비지 않는다.

export const options = singleOptions('GET /api/purchases/me');

export default function () {
    const buyer = getBuyerSession();

    const res = http.get(`${COMMERCE_BASE_URL}/api/purchases/me`, {
        headers: authHeaders(buyer),
        tags: { name: 'GET /api/purchases/me' },
    });

    check(res, { '내 구매 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
