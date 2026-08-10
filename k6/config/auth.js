import http from 'k6/http';
import { check } from 'k6';
import {
    IDENTITY_BASE_URL,
    THROUGH_GATEWAY,
    JSON_HEADERS,
} from './environment.js';

// Auth 외의 모든 BC는 @AuthUser 인증이 필요하므로, 각 VU가 자기 계정을 하나 만들어 재사용한다.
// k6는 VU마다 독립된 JS 런타임을 쓰므로 모듈 스코프 변수 = VU별 상태가 된다.

const PASSWORD = 'LoadTest1234!';
const PHONE = '010-1234-5678';

function uniqueEmail() {
    return `loadtest-${__VU}-${Date.now()}@example.com`;
}

// 회원가입 + 로그인 → { email, memberId, accessToken }
export function signUpAndLogin() {
    const email = uniqueEmail();

    const signUpRes = http.post(
        `${IDENTITY_BASE_URL}/api/auth/signup`,
        JSON.stringify({
            email,
            password: PASSWORD,
            name: '부하테스트',
            defaultShippingAddress: '서울특별시 강남구 테헤란로 123',
            phoneNumber: PHONE,
        }),
        { headers: JSON_HEADERS, tags: { name: 'SETUP POST /api/auth/signup' } }
    );

    check(signUpRes, { '[setup] 회원가입 성공': (r) => r.status === 200 });

    const loginRes = http.post(
        `${IDENTITY_BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password: PASSWORD }),
        { headers: JSON_HEADERS, tags: { name: 'SETUP POST /api/auth/login' } }
    );

    check(loginRes, { '[setup] 로그인 성공': (r) => r.status === 200 });

    return {
        email,
        memberId: signUpRes.json('data.memberId'),
        accessToken: loginRes.json('data.accessToken'),
    };
}

// 인증 헤더를 모드에 맞게 생성한다.
//  - gateway: 게이트웨이가 Bearer 토큰을 검증하고 X-Authenticated-Member-Id를 주입한다.
//             (클라이언트가 보낸 해당 헤더는 게이트웨이가 무조건 제거하므로 보낼 필요 없음)
//  - direct : 게이트웨이가 없으므로 그 주입 역할을 스크립트가 대신한다.
export function authHeaders(session, extra) {
    const base = THROUGH_GATEWAY
        ? { Authorization: `Bearer ${session.accessToken}` }
        : { 'X-Authenticated-Member-Id': String(session.memberId) };

    return Object.assign(base, extra || {});
}

export function authJsonHeaders(session) {
    return authHeaders(session, JSON_HEADERS);
}

// 판매자 등록. 상품 등록 계열 시나리오에서 선행으로 1회만 호출한다.
export function registerSeller(session) {
    const res = http.post(
        `${IDENTITY_BASE_URL}/api/members/me/seller`,
        JSON.stringify({ bank: '국민은행', account: '123456-78-901234' }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'SETUP POST /api/members/me/seller' },
        }
    );

    // 고정 계정은 한 번 판매자가 되면 그 상태가 유지된다.
    // 재실행 시 ALREADY_SELLER(400)가 오는데 이는 정상이므로 성공으로 본다.
    check(res, {
        '[setup] 판매자 상태 확보': (r) =>
            r.status === 200 || (r.status === 400 && r.body.includes('MB-004')),
    });
    return res;
}

// VU별 세션 캐시. 첫 반복에서만 계정을 만들고 이후 반복은 재사용한다.
let cachedSession = null;

// 회원가입으로 새 계정을 만들어 쓰는 세션.
// 계정 생성·소모 자체가 측정 대상인 스크립트에서만 사용한다.
// 그 외에는 getSession()(고정 계정)을 쓸 것.
export function getFreshSession() {
    if (cachedSession === null) {
        const session = signUpAndLogin();

        // 계정 생성에 실패한 세션을 캐싱하면 그 VU는 테스트가 끝날 때까지
        // 인증 없는 요청을 반복해 서버를 때린다. 즉시 중단시킨다.
        if (!session.accessToken) {
            throw new Error(
                'VU 계정 생성 실패. 서버 부하 또는 장애 가능성이 있습니다. ' +
                'VU 수를 낮추거나 서버 상태를 확인하세요.'
            );
        }

        cachedSession = session;
    }
    return cachedSession;
}

// ---------------------------------------------------------------------------
// 구매(write) 테스트용 고정 계정
//
// 구매는 지갑 잔액이 필요한데, 잔액을 채우는 API가 없다.
//   - POST /internal/deposits : 지갑 "생성"만 하고 잔액은 0
//   - POST /api/payments/confirm : 실제 토스 API 호출 → 부하테스트에서 사용 금지
// 그래서 잔액은 sql/seed-wallet.sql로 DB에 직접 넣는다.
//
// 문제는 실행 중 생성되는 계정은 memberId를 미리 알 수 없어 SQL 대상이 될 수 없다는 점이다.
// 따라서 구매 시나리오는 seed.js가 미리 만들어 둔 결정적 이메일 계정을 사용한다.
// ---------------------------------------------------------------------------

export const BUYER_EMAIL_PREFIX = 'loadtest-buyer-';
export const BUYER_PASSWORD = PASSWORD;

export function buyerEmail(index) {
    return `${BUYER_EMAIL_PREFIX}${index}@example.com`;
}

// 가입 없이 로그인만 수행한다. 계정이 없으면 실패하므로 seed.js를 먼저 돌려야 한다.
export function loginAsBuyer(index) {
    const email = buyerEmail(index);

    const res = http.post(
        `${IDENTITY_BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password: BUYER_PASSWORD }),
        {
            headers: JSON_HEADERS,
            tags: { name: 'SETUP POST /api/auth/login (buyer)' },
        }
    );

    if (res.status !== 200) {
        throw new Error(
            `구매용 계정 로그인 실패: ${email} (HTTP ${res.status}). ` +
            'seed.js를 먼저 실행하고 sql/seed-wallet.sql로 잔액을 채웠는지 확인하세요.'
        );
    }

    // direct 모드는 헤더 주입 방식이라 memberId가 필요한데,
    // 로그인 응답(TokenResponse)에는 accessToken/tokenType/expiresIn만 있고 memberId가 없다.
    // 따라서 seed.js가 출력한 값을 -e BUYER_MEMBER_IDS=... 로 넘겨야 한다.
    let memberId = null;

    if (!THROUGH_GATEWAY) {
        const ids = (__ENV.BUYER_MEMBER_IDS || '')
            .split(',')
            .filter((v) => v !== '');

        if (ids.length < index) {
            throw new Error(
                `direct 모드에서는 BUYER_MEMBER_IDS가 필요합니다 (VU ${index}번째 값 없음). ` +
                'seed.js 출력의 BUYER_MEMBER_IDS 값을 -e 로 넘기거나, gateway 모드로 실행하세요.'
            );
        }
        memberId = Number(ids[index - 1]);
    }

    return { email, memberId, accessToken: res.json('data.accessToken') };
}

// seed.js가 만들어 둔 고정 계정 수. 시딩 때 준 BUYER_COUNT와 같아야 한다.
export const BUYER_COUNT = Number(__ENV.BUYER_COUNT || 200);

// 고정 계정 세션 캐시 (VU별).
//
// 부하 테스트 중 회원가입을 하지 않는 이유:
//   회원가입은 identity-service가 commerce-service의 /internal/deposits 를 호출해
//   지갑을 만드는 구조다(MemberService.createProfile). 즉 측정 대상과 무관한
//   서비스 간 호출이 섞여, commerce가 바쁘면 회원가입이 통째로 실패한다.
//   실제로 VU 50 측정에서 회원가입 성공률이 9%까지 떨어졌다.
//
//   그래서 조회·수정 계열은 미리 만들어 둔 고정 계정으로 로그인만 한다.
//   계정 생성 자체가 측정 대상인 스크립트(auth/signup, auth/withdraw,
//   member/seller-register, member/seller-unregister)만 예외적으로 가입을 수행한다.
let cachedBuyerSession = null;

export function getBuyerSession() {
    if (cachedBuyerSession === null) {
        // VU 수가 계정 수를 넘어도 순환하도록 나머지 연산을 쓴다.
        const index = ((__VU - 1) % BUYER_COUNT) + 1;
        cachedBuyerSession = loginAsBuyer(index);
    }
    return cachedBuyerSession;
}

// 인증이 필요한 대부분의 시나리오가 쓰는 기본 세션.
// 이제 회원가입 없이 고정 계정 로그인만 수행한다.
export function getSession() {
    return getBuyerSession();
}
