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
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/auth/logout
//
// 로그아웃은 저장된 Refresh Token을 삭제하는 단순 작업이라 반복 호출해도 성공한다.
// VU마다 최초 1회만 로그인하고(PREP), 이후 로그아웃만 반복한다.

export const options = singleOptions('POST /api/auth/logout');

export default function () {
    const session = getSession();

    const res = http.post(`${IDENTITY_BASE_URL}/api/auth/logout`, null, {
        headers: authHeaders(session),
        tags: { name: 'POST /api/auth/logout' },
    });

    check(res, { '로그아웃 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
