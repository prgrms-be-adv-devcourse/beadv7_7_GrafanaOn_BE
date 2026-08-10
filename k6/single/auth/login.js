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
    buyerEmail,
} from '../../config/auth.js';

// 측정 대상: POST /api/auth/login
//
// BCrypt 검증이 포함되어 의도적으로 느리다. 목표 500ms.
// seed.js가 만든 고정 계정을 재사용한다(계정 생성 비용을 측정에서 제외하기 위함).
// VU 수가 BUYER_COUNT를 넘어도 나머지 연산으로 순환하므로 실패하지 않는다.

export const options = singleOptions('POST /api/auth/login', 500);

const BUYER_COUNT = Number(__ENV.BUYER_COUNT || 10);

export default function () {
    const index = ((__VU - 1) % BUYER_COUNT) + 1;

    const res = http.post(
        `${IDENTITY_BASE_URL}/api/auth/login`,
        JSON.stringify({ email: buyerEmail(index), password: BUYER_PASSWORD }),
        { headers: JSON_HEADERS, tags: { name: 'POST /api/auth/login' } }
    );

    check(res, {
        '로그인 200': (r) => r.status === 200,
        'accessToken 존재': (r) => !!r.json('data.accessToken'),
    });
    sleep(THINK_TIME);
}
