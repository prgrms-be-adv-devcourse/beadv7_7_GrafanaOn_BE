import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    registerSeller,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/products/me
// 판매자 본인의 상품 목록. 조회라 반복 제약이 없다.

export const options = singleOptions('GET /api/products/me');

let sellerRegistered = false;

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    const res = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/products/me' },
    });

    check(res, { '내 상품 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
