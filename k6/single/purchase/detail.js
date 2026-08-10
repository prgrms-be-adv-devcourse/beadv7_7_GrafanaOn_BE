import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getBuyerSession,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/purchases/{purchaseId}
//
// 조회라 반복 제약이 없다. VU별로 구매를 1건 만들어 두고(PREP) 그것만 계속 조회한다.
// 상품 소모는 VU당 1건뿐이다.

export const options = singleOptions('GET /api/purchases/:id');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error(
            '-e PRODUCT_IDS=... 가 필요합니다. ' +
            'sql/get-product-ids.sql 의 IMMEDIATE 목록을 사용하세요.'
        );
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

let purchaseId = null;

export default function (data) {
    const buyer = getBuyerSession();

    if (purchaseId === null) {
        const productId = data.productIds[(__VU - 1) % data.productIds.length];

        const createRes = http.post(
            `${COMMERCE_BASE_URL}/api/purchases`,
            JSON.stringify({ productId, delivery: '서울특별시 강남구 테헤란로 123' }),
            { headers: authJsonHeaders(buyer), tags: { name: 'PREP POST /api/purchases' } }
        );

        purchaseId = createRes.json('data.id');
        if (!purchaseId) throw new Error('측정용 구매 생성에 실패했습니다.');
    }

    const res = http.get(`${COMMERCE_BASE_URL}/api/purchases/${purchaseId}`, {
        headers: authHeaders(buyer),
        tags: { name: 'GET /api/purchases/:id' },
    });

    check(res, { '구매 상세 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
