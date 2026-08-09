// TARGET: 'gateway'(기본) | 'direct'
//  - gateway: 실제 사용자 경로. 클라이언트 → 게이트웨이(8080) → 각 서비스
//             인증 API는 Authorization: Bearer 사용
//             (게이트웨이가 토큰을 검증한 뒤 X-Authenticated-Member-Id를 주입한다)
//  - direct : 게이트웨이를 빼고 각 서비스 단독 측정. 병목 분리용 기준선.
//             게이트웨이의 헤더 주입 역할을 스크립트가 흉내낸다.
export const TARGET = __ENV.TARGET || 'gateway';
export const THROUGH_GATEWAY = TARGET === 'gateway';

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
const IDENTITY_URL = __ENV.IDENTITY_URL || 'http://localhost:8081';
const COMMERCE_URL = __ENV.COMMERCE_URL || 'http://localhost:8082';

// TARGET 하나로 URL과 인증 방식이 함께 결정된다. (따로 설정하다 어긋날 여지 제거)
export const IDENTITY_BASE_URL = THROUGH_GATEWAY ? GATEWAY_URL : IDENTITY_URL;
export const COMMERCE_BASE_URL = THROUGH_GATEWAY ? GATEWAY_URL : COMMERCE_URL;

// TEST_TYPE: 'load' | 'stress'
export const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// THINK_TIME: 요청 사이 대기(초). 실사용자 재현은 1, 최대 처리량 측정은 0.
//
// 주의: TPS = VU / (응답시간 + THINK_TIME)
//   50 VU + 100ms 응답 + 1s 대기 → 약  45 TPS
//   50 VU + 100ms 응답 + 0s 대기 → 약 500 TPS (목표치)
// 목표 500 TPS를 실제로 검증하려면 THINK_TIME=0으로 돌려야 한다.
export const THINK_TIME =
    __ENV.THINK_TIME === undefined ? 1 : Number(__ENV.THINK_TIME);

export const THRESHOLDS = {
    http_req_duration: ['p(95)<100'], // 목표 응답시간 100ms (구글 권장, 네트워크 포함)
    http_req_failed: ['rate<0.01'],
};

export const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Refresh Token 쿠키 이름. 서버의 AUTH_REFRESH_COOKIE_NAME와 같아야 한다.
export const REFRESH_COOKIE_NAME = __ENV.REFRESH_COOKIE_NAME || 'refreshToken';
