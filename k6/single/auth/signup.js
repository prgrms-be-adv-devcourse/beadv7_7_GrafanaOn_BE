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
    signUpAndLogin,
    authHeaders,
    authJsonHeaders,
    buyerEmail,
    BUYER_PASSWORD,
} from '../../config/auth.js';

// 측정 대상: POST /api/auth/signup
//
// BCrypt 해시 생성이 포함되어 의도적으로 느린 API다.
// 목표를 500ms로 둔 이유는 100ms 기준이 이 API에는 맞지 않기 때문이다.
// email에 unique 제약이 있어 매 반복 새 값을 만든다.

export const options = singleOptions('POST /api/auth/signup', 500);

export default function () {
    const email = `loadtest-${__VU}-${__ITER}-${Date.now()}@example.com`;

    const res = http.post(
        `${IDENTITY_BASE_URL}/api/auth/signup`,
        JSON.stringify({
            email,
            password: 'LoadTest1234!',
            name: '부하테스트',
            defaultShippingAddress: '서울특별시 강남구 테헤란로 123',
            phoneNumber: '010-1234-5678',
        }),
        { headers: JSON_HEADERS, tags: { name: 'POST /api/auth/signup' } }
    );

    check(res, { '회원가입 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
