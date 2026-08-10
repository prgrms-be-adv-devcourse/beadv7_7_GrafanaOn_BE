import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    THINK_TIME,
    JSON_HEADERS,
    REFRESH_COOKIE_NAME,
    THROUGH_GATEWAY,
} from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    signUpAndLogin,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: DELETE /api/auth/withdraw
//
// 탈퇴는 계정당 1회만 가능하므로 매 반복 새 계정을 만들어 소모한다.
// 계정 생성(가입 + 로그인)은 PREP 태그로 분리되어 측정값에 섞이지 않는다.
//
// ⚠️ 반복 횟수만큼 탈퇴 계정이 쌓인다. 테스트 후 sql/cleanup-runtime.sql 로 정리할 것.

export const options = singleOptions('DELETE /api/auth/withdraw');

export default function () {
    const session = signUpAndLogin();

    const res = http.del(`${IDENTITY_BASE_URL}/api/auth/withdraw`, null, {
        headers: authHeaders(session),
        tags: { name: 'DELETE /api/auth/withdraw' },
    });

    check(res, { '탈퇴 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
