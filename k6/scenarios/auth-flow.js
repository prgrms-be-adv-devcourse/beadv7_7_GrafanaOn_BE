import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    THROUGH_GATEWAY,
    THINK_TIME,
    THRESHOLDS,
    JSON_HEADERS,
    REFRESH_COOKIE_NAME,
} from '../config/environment.js';
import { stages } from '../config/stages.js';

// Auth 시나리오: 회원가입 → 로그인 → 토큰 재발급 → 로그아웃
// 이 시나리오만 매 반복 새 계정을 만든다(가입 API 자체가 측정 대상이므로).

export const options = { stages, thresholds: THRESHOLDS };

const PASSWORD = 'LoadTest1234!';
const PHONE = '010-1234-5678';

export default function () {
    // email은 unique 제약이 있으므로 VU/반복/시각을 조합해 충돌을 피한다.
    const email = `loadtest-${__VU}-${__ITER}-${Date.now()}@example.com`;

    // 1. 회원가입 (공개 API)
    const signUpRes = http.post(
        `${IDENTITY_BASE_URL}/api/auth/signup`,
        JSON.stringify({
            email,
            password: PASSWORD,
            name: '부하테스트',
            defaultShippingAddress: '서울특별시 강남구 테헤란로 123',
            phoneNumber: PHONE,
        }),
        { headers: JSON_HEADERS, tags: { name: 'POST /api/auth/signup' } }
    );
    check(signUpRes, { '회원가입 200': (r) => r.status === 200 });

    const memberId = signUpRes.json('data.memberId');
    sleep(THINK_TIME);

    // 2. 로그인 (공개 API. Refresh Token 쿠키는 k6가 VU별로 자동 보관)
    const loginRes = http.post(
        `${IDENTITY_BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password: PASSWORD }),
        { headers: JSON_HEADERS, tags: { name: 'POST /api/auth/login' } }
    );
    check(loginRes, {
        '로그인 200': (r) => r.status === 200,
        'accessToken 존재': (r) => !!r.json('data.accessToken'),
    });

    const accessToken = loginRes.json('data.accessToken');

    // 로그인 응답의 Set-Cookie에서 Refresh Token 값을 직접 꺼낸다.
    //
    // 왜 k6의 자동 쿠키 저장소를 안 쓰는가:
    //   서버는 이 쿠키를 Secure 플래그와 함께 내려준다(AUTH_COOKIE_SECURE=true).
    //   그런데 부하테스트는 Cloudflare를 우회하려고 평문 HTTP로 오리진에 직접 붙기 때문에,
    //   k6가 브라우저 규칙대로 Secure 쿠키를 HTTP 요청에 실어주지 않아 재발급이 401이 된다.
    //   실사용(HTTPS)에서는 정상이므로, 테스트 환경 문제를 스크립트에서 우회한다.
    const refreshCookie = loginRes.cookies[REFRESH_COOKIE_NAME];
    const refreshToken =
        refreshCookie && refreshCookie.length > 0 ? refreshCookie[0].value : null;

    check(loginRes, { 'refreshToken 쿠키 발급': () => refreshToken !== null });

    sleep(THINK_TIME);

    // 3. 토큰 재발급 (공개 API. Refresh Token 쿠키를 명시적으로 실어 보낸다)
    const reissueRes = http.post(`${IDENTITY_BASE_URL}/api/auth/reissue`, null, {
        headers: Object.assign({}, JSON_HEADERS, {
            Cookie: `${REFRESH_COOKIE_NAME}=${refreshToken}`,
        }),
        tags: { name: 'POST /api/auth/reissue' },
    });
    check(reissueRes, { '재발급 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 4. 로그아웃 (인증 필요 — 모드에 따라 인증 방식이 다르다)
    //    gateway: 게이트웨이가 Bearer 토큰을 검증하고 헤더를 주입한다.
    //    direct : 게이트웨이가 없으므로 그 주입 역할을 스크립트가 대신한다.
    const logoutHeaders = THROUGH_GATEWAY
        ? { Authorization: `Bearer ${accessToken}` }
        : { 'X-Authenticated-Member-Id': String(memberId) };

    const logoutRes = http.post(`${IDENTITY_BASE_URL}/api/auth/logout`, null, {
        headers: logoutHeaders,
        tags: { name: 'POST /api/auth/logout' },
    });
    check(logoutRes, { '로그아웃 200': (r) => r.status === 200 });

    sleep(THINK_TIME);
}
