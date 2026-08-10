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
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/scraps/{productId}
//
// (회원, 상품) 유니크 제약이 있어 같은 조합을 두 번 담을 수 없다.
// 상품을 순차로 골라 VU 안에서 중복이 나지 않게 한다.
// ⚠️ VU당 반복 횟수가 상품 수를 넘으면 실패가 발생한다. PRODUCT_IDS를 넉넉히 줄 것.

export const options = singleOptions('POST /api/scraps/:productId');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error(
            '-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.'
        );
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const session = getSession();
    const productId = data.productIds[__ITER % data.productIds.length];

    const res = http.post(`${IDENTITY_BASE_URL}/api/scraps/${productId}`, null, {
        headers: authHeaders(session),
        tags: { name: 'POST /api/scraps/:productId' },
    });

    check(res, { '스크랩 추가 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
