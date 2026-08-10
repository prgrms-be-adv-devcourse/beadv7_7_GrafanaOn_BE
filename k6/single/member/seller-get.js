import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    COMMERCE_BASE_URL,
    THINK_TIME,
} from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    registerSeller,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/members/me/seller
// 계좌번호 마스킹 처리 비용을 측정한다. 조회라 반복 제약이 없다.

export const options = singleOptions('GET /api/members/me/seller');

let sellerRegistered = false;

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    const res = http.get(`${IDENTITY_BASE_URL}/api/members/me/seller`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/members/me/seller' },
    });

    check(res, { '판매자 계좌 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
