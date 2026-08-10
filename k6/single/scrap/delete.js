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

// 측정 대상: DELETE /api/scraps/{productId}
//
// 삭제하려면 먼저 담겨 있어야 한다. 매 반복 담고(PREP) 삭제를 측정한다.
// 담기/삭제가 짝을 이루므로 데이터가 쌓이지 않는다.

export const options = singleOptions('DELETE /api/scraps/:productId');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const session = getSession();
    const productId = data.productIds[__ITER % data.productIds.length];

    http.post(`${IDENTITY_BASE_URL}/api/scraps/${productId}`, null, {
        headers: authHeaders(session),
        tags: { name: 'PREP POST /api/scraps/:productId' },
    });

    const res = http.del(`${IDENTITY_BASE_URL}/api/scraps/${productId}`, null, {
        headers: authHeaders(session),
        tags: { name: 'DELETE /api/scraps/:productId' },
    });

    check(res, { '스크랩 삭제 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
