import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/payments/{paymentId}
//
// 조회라 반복 제약이 없다. VU별로 결제를 1건 준비해 두고(PREP) 그것만 계속 조회한다.

export const options = singleOptions('GET /api/payments/:id');

let paymentId = null;

export default function () {
    const session = getSession();

    if (paymentId === null) {
        const chargeRes = http.post(
            `${COMMERCE_BASE_URL}/api/payments/charge`,
            JSON.stringify({ amount: 10000 }),
            {
                headers: authJsonHeaders(session),
                tags: { name: 'PREP POST /api/payments/charge' },
            }
        );

        paymentId = chargeRes.json('data.paymentId') || chargeRes.json('data.id');
        if (!paymentId) {
            throw new Error(
                `측정용 결제 생성에 실패했습니다. 응답: ${chargeRes.body}`
            );
        }
    }

    const res = http.get(`${COMMERCE_BASE_URL}/api/payments/${paymentId}`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/payments/:id' },
    });

    check(res, { '결제 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
