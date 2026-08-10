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

// 측정 대상: POST /api/auth/reissue
//
// 재발급은 기존 Refresh Token을 새 것으로 교체한다. 따라서 한 번 쓴 토큰은 재사용할 수 없다.
// VU마다 최초 1회 로그인해 토큰을 얻고, 이후에는 재발급 응답으로 받은 새 토큰을 이어서 쓴다.
// 로그인은 PREP 태그로 분리되어 측정값에 섞이지 않는다.
//
// 쿠키를 헤더로 직접 실어 보내는 이유는 CAUTION.md 8번 참고
// (Secure 쿠키 + 평문 HTTP 조합에서 k6가 쿠키를 자동 전송하지 않는다).

export const options = singleOptions('POST /api/auth/reissue');

const BUYER_COUNT = Number(__ENV.BUYER_COUNT || 10);

let refreshToken = null;

export default function () {
    if (refreshToken === null) {
        const index = ((__VU - 1) % BUYER_COUNT) + 1;
        const loginRes = http.post(
            `${IDENTITY_BASE_URL}/api/auth/login`,
            JSON.stringify({ email: buyerEmail(index), password: BUYER_PASSWORD }),
            { headers: JSON_HEADERS, tags: { name: 'PREP POST /api/auth/login' } }
        );

        const cookie = loginRes.cookies[REFRESH_COOKIE_NAME];
        refreshToken = cookie && cookie.length > 0 ? cookie[0].value : null;

        if (refreshToken === null) {
            throw new Error('로그인 응답에서 Refresh Token 쿠키를 찾지 못했습니다.');
        }
    }

    const res = http.post(`${IDENTITY_BASE_URL}/api/auth/reissue`, null, {
        headers: Object.assign({}, JSON_HEADERS, {
            Cookie: `${REFRESH_COOKIE_NAME}=${refreshToken}`,
        }),
        tags: { name: 'POST /api/auth/reissue' },
    });

    check(res, { '재발급 200': (r) => r.status === 200 });

    // 재발급이 성공하면 토큰이 교체된다. 다음 반복을 위해 새 토큰으로 갱신한다.
    const rotated = res.cookies[REFRESH_COOKIE_NAME];
    if (rotated && rotated.length > 0) {
        refreshToken = rotated[0].value;
    }

    sleep(THINK_TIME);
}
