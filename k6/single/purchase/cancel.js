import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getBuyerSession,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: DELETE /api/purchases/{purchaseId}/cancel
//
// 취소는 구매 1건당 1회뿐이므로 매 반복 구매를 만들고(PREP) 취소를 측정한다.
//
// ⚠️ 취소로 상품이 ON_SALE 로 복구되는지는 도메인 정책에 달려 있다.
//    복구되지 않으면 반복 횟수만큼 상품이 소모되므로, 먼저 소규모로 확인할 것.

export const options = singleOptions('DELETE /api/purchases/:id/cancel');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error(
            '-e PRODUCT_IDS=... 가 필요합니다. ' +
            'sql/get-product-ids.sql 의 IMMEDIATE 목록을 사용하세요.'
        );
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const buyer = getBuyerSession();

    const index = (__VU - 1) * 10007 + __ITER;
    const productId = data.productIds[index % data.productIds.length];

    const createRes = http.post(
        `${COMMERCE_BASE_URL}/api/purchases`,
        JSON.stringify({ productId, delivery: '서울특별시 강남구 테헤란로 123' }),
        { headers: authJsonHeaders(buyer), tags: { name: 'PREP POST /api/purchases' } }
    );

    check(createRes, { '[선행] 구매 생성 201': (r) => r.status === 201 });

    const purchaseId = createRes.json('data.id');
    if (!purchaseId) {
        // 선행 요청이 실패해도 즉시 다음 반복으로 가지 않는다.
        // sleep 없이 반환하면 실패한 VU가 초당 수백 건으로 폭주한다.
        sleep(THINK_TIME);
        return;
    }

    const res = http.del(
        `${COMMERCE_BASE_URL}/api/purchases/${purchaseId}/cancel`,
        null,
        { headers: authHeaders(buyer), tags: { name: 'DELETE /api/purchases/:id/cancel' } }
    );

    check(res, { '구매 취소 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
