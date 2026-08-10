import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import { getSession, authHeaders, authJsonHeaders } from '../../config/auth.js';

// 측정 대상: DELETE /api/carts/items/all
// 비울 항목이 있어야 의미가 있으므로 매 반복 몇 건 담고(PREP) 전체 삭제를 측정한다.

export const options = singleOptions('DELETE /api/carts/items/all');

const PREP_COUNT = Number(__ENV.PREP_COUNT || 5);

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const session = getSession();

    for (let i = 0; i < PREP_COUNT && i < data.productIds.length; i++) {
        const productId = data.productIds[(__ITER * PREP_COUNT + i) % data.productIds.length];
        http.post(
            `${COMMERCE_BASE_URL}/api/carts/items`,
            JSON.stringify({ productId }),
            { headers: authJsonHeaders(session), tags: { name: 'PREP POST /api/carts/items' } }
        );
    }

    const res = http.del(`${COMMERCE_BASE_URL}/api/carts/items/all`, null, {
        headers: authHeaders(session),
        tags: { name: 'DELETE /api/carts/items/all' },
    });

    check(res, { '전체 비우기 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
