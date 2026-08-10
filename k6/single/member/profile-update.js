import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    COMMERCE_BASE_URL,
    THINK_TIME,
} from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: PATCH /api/members/profile/me
//
// 닉네임에 unique 제약이 있고 매번 중복 검사 쿼리가 돈다.
// 회원 수가 늘수록 이 검사가 무거워지는지 보는 것이 목적이다.
// RUN_ID로 이전 실행분과의 충돌을 피한다.

export const options = singleOptions('PATCH /api/members/profile/me');

const RUN_ID = Date.now().toString(36).slice(-5);

export default function () {
    const session = getSession();

    const res = http.patch(
        `${IDENTITY_BASE_URL}/api/members/profile/me`,
        JSON.stringify({
            defaultShippingAddress: '서울특별시 송파구 올림픽로 300',
            phoneNumber: '010-9876-5432',
            nickname: `single_${RUN_ID}_${__VU}_${__ITER}`,
        }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'PATCH /api/members/profile/me' },
        }
    );

    check(res, { '프로필 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
