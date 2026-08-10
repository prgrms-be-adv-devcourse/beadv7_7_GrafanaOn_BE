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
    registerSeller,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: PATCH /api/members/me/seller
//
// 계좌 정보를 덮어쓰는 멱등 연산이라 반복 호출이 가능하다.
// 계좌번호 암호화 비용을 측정한다. 판매자 등록은 VU별 1회만 수행(PREP).

export const options = singleOptions('PATCH /api/members/me/seller');

let sellerRegistered = false;

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    const res = http.patch(
        `${IDENTITY_BASE_URL}/api/members/me/seller`,
        JSON.stringify({ bank: '신한은행', account: '110-123-456789' }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'PATCH /api/members/me/seller' },
        }
    );

    check(res, { '계좌 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
