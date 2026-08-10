import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/payments/charge
//
// 충전 준비 단계다. 결제 레코드를 생성하고 orderId를 발급한다.
//
// ⚠️ 반복 횟수만큼 미완료 결제 레코드가 쌓인다. 테스트 후 정리가 필요하다.
// ⚠️ POST /api/payments/confirm 은 실제 토스 API를 호출하므로 절대 측정하지 말 것.
//    이 폴더에 confirm 스크립트가 없는 이유다. (CAUTION.md 1번)

export const options = singleOptions('POST /api/payments/charge');

export default function () {
    const session = getSession();

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/payments/charge`,
        JSON.stringify({ amount: 10000 }),
        { headers: authJsonHeaders(session), tags: { name: 'POST /api/payments/charge' } }
    );

    check(res, { '충전 준비 201': (r) => r.status === 201 });
    sleep(THINK_TIME);
}
