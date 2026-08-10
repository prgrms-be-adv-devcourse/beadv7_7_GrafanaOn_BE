import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    COMMERCE_BASE_URL,
    THINK_TIME,
} from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    signUpAndLogin,
    registerSeller,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: DELETE /api/members/me/seller
//
// 해지는 계정당 1회만 가능하므로 매 반복 새 계정을 만들고 판매자로 등록한 뒤 해지한다.
// 준비 단계(가입/로그인/판매자 등록)는 PREP 태그로 분리된다.
//
// ⚠️ 판매 중인 상품이 있으면 해지가 거부된다(WITHDRAWAL_FAILED).
//    여기서 만드는 계정은 상품이 없어 해당되지 않는다.

export const options = singleOptions('DELETE /api/members/me/seller');

export default function () {
    const session = signUpAndLogin();
    registerSeller(session);

    const res = http.del(`${IDENTITY_BASE_URL}/api/members/me/seller`, null, {
        headers: authHeaders(session),
        tags: { name: 'DELETE /api/members/me/seller' },
    });

    check(res, { '판매자 해지 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
