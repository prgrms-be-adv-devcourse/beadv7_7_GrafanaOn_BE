import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/products/{productId}
//
// 상품에 딸린 이미지·스토리를 함께 조회하므로 N+1 발생 여부를 보는 것이 목적이다.
// PREPARING 상품은 판매자 본인에게만 보이므로 ON_SALE 상품만 대상으로 한다.

export const options = singleOptions('GET /api/products/:id');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const session = getSession();
    const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];

    const res = http.get(`${COMMERCE_BASE_URL}/api/products/${productId}`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/products/:id' },
    });

    check(res, { '상품 상세 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
