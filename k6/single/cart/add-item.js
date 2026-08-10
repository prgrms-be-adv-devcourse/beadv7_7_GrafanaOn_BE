import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import { getSession, authHeaders, authJsonHeaders } from '../../config/auth.js';

// 측정 대상: POST /api/carts/items
//
// (장바구니, 상품) 유니크 제약이 있어 같은 상품을 두 번 담을 수 없다.
// 상품을 순차로 골라 중복을 피하고, 측정 후 장바구니를 비워(PREP) 다음 반복을 준비한다.

export const options = singleOptions('POST /api/carts/items');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const session = getSession();
    const productId = data.productIds[__ITER % data.productIds.length];

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/carts/items`,
        JSON.stringify({ productId }),
        { headers: authJsonHeaders(session), tags: { name: 'POST /api/carts/items' } }
    );

    check(res, { '장바구니 담기 200': (r) => r.status === 200 });

    // 다음 반복에서 같은 상품을 다시 담을 수 있도록 비운다.
    http.del(`${COMMERCE_BASE_URL}/api/carts/items/all`, null, {
        headers: authHeaders(session),
        tags: { name: 'PREP DELETE /api/carts/items/all' },
    });

    sleep(THINK_TIME);
}
