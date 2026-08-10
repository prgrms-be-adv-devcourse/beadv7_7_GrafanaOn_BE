import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import { getSession, authHeaders, authJsonHeaders } from '../../config/auth.js';

// 측정 대상: GET /api/carts
//
// 장바구니 항목마다 상품 정보를 조회해 붙이므로 N+1 발생 여부를 보는 것이 목적이다.
// 항목 수가 많을수록 드러나므로 VU별로 미리 여러 건 담아둔다(PREP).

export const options = singleOptions('GET /api/carts');

const PREP_COUNT = Number(__ENV.PREP_COUNT || 15);

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

let prepared = false;

export default function (data) {
    const session = getSession();

    if (!prepared) {
        for (let i = 0; i < PREP_COUNT && i < data.productIds.length; i++) {
            http.post(
                `${COMMERCE_BASE_URL}/api/carts/items`,
                JSON.stringify({ productId: data.productIds[i] }),
                {
                    headers: authJsonHeaders(session),
                    tags: { name: 'PREP POST /api/carts/items' },
                }
            );
        }
        prepared = true;
    }

    const res = http.get(`${COMMERCE_BASE_URL}/api/carts`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/carts' },
    });

    check(res, { '장바구니 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
