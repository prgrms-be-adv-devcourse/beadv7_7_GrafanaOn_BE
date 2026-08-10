import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/offers/snapshot
//
// 같은 (작성자, 상품) 조합이면 기존 스냅샷을 갱신하는 구조라 반복 호출이 가능하다.
// 대상 상품은 status=ON_SALE 이어야 한다. PREPARING 이면 OF-008 로 실패한다.

export const options = singleOptions('POST /api/offers/snapshot');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const buyer = getSession();
    const productId = data.productIds[__ITER % data.productIds.length];

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/offers/snapshot`,
        JSON.stringify({ productId }),
        { headers: authJsonHeaders(buyer), tags: { name: 'POST /api/offers/snapshot' } }
    );

    check(res, { '스냅샷 생성 201': (r) => r.status === 201 });
    sleep(THINK_TIME);
}
