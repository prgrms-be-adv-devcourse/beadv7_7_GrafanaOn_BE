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
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/members/me/seller
//
// 이미 판매자인 계정에 다시 호출하면 실패하므로(ALREADY_SELLER)
// 매 반복 새 계정을 만들어 소모한다. 계정 생성은 PREP 태그로 분리한다.
// 계좌번호 암호화 비용이 이 API에 포함된다.
//
// ⚠️ 반복 횟수만큼 계정이 쌓인다. 테스트 후 sql/cleanup-runtime.sql 로 정리할 것.

export const options = singleOptions('POST /api/members/me/seller');

export default function () {
    const session = signUpAndLogin();

    const res = http.post(
        `${IDENTITY_BASE_URL}/api/members/me/seller`,
        JSON.stringify({ bank: '국민은행', account: '123456-78-901234' }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'POST /api/members/me/seller' },
        }
    );

    check(res, { '판매자 등록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
