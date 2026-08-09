import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import {
    getSession,
    authHeaders,
    authJsonHeaders,
    registerSeller,
} from '../config/auth.js';

// Member 시나리오: 프로필 조회/수정 + 판매자 계좌 조회/수정
// 판매자 등록(POST /me/seller)은 중복 호출 시 ALREADY_SELLER로 실패하므로 VU별 1회만 수행한다.

export const options = { stages, thresholds: THRESHOLDS };

let sellerRegistered = false;

// 닉네임은 unique 제약이 있고, 이전 실행에서 만든 닉네임이 DB에 그대로 남아 있다.
// VU/반복 번호만 쓰면 재실행 시 충돌하므로 실행 단위 고유값을 붙인다.
const RUN_ID = Date.now().toString(36).slice(-5);

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    // 1. 내 프로필 조회
    const profileRes = http.get(`${IDENTITY_BASE_URL}/api/members/profile`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/members/profile' },
    });
    check(profileRes, { '프로필 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 2. 프로필 수정
    // nickname은 unique 제약이 있다. RUN_ID로 실행 간 충돌을, VU/반복 번호로 실행 내
    // 충돌을 각각 피한다.
    const updateRes = http.patch(
        `${IDENTITY_BASE_URL}/api/members/profile/me`,
        JSON.stringify({
            defaultShippingAddress: '서울특별시 송파구 올림픽로 300',
            phoneNumber: '010-9876-5432',
            nickname: `load_${RUN_ID}_${__VU}_${__ITER}`,
        }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'PATCH /api/members/profile/me' },
        }
    );
    check(updateRes, { '프로필 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 3. 내 판매자 계좌 조회 (계좌번호 마스킹 처리 구간)
    const accountRes = http.get(`${IDENTITY_BASE_URL}/api/members/me/seller`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/members/me/seller' },
    });
    check(accountRes, { '판매자 계좌 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 4. 판매자 계좌 수정 (멱등하므로 반복 호출 가능. 계좌번호 암호화 구간)
    const updateAccountRes = http.patch(
        `${IDENTITY_BASE_URL}/api/members/me/seller`,
        JSON.stringify({ bank: '신한은행', account: '110-123-456789' }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'PATCH /api/members/me/seller' },
        }
    );
    check(updateAccountRes, { '계좌 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
