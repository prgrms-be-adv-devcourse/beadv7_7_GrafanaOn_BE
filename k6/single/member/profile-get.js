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
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/members/profile
// 조회 전용이라 반복에 제약이 없다.

export const options = singleOptions('GET /api/members/profile');

export default function () {
    const session = getSession();

    const res = http.get(`${IDENTITY_BASE_URL}/api/members/profile`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/members/profile' },
    });

    check(res, { '프로필 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
