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

// 측정 대상: GET /api/scraps
//
// 페이징 조회다. 목록이 비어 있으면 부하 의미가 없으므로
// VU별로 먼저 몇 건 담아둔 뒤(PREP) 조회만 반복한다.

export const options = singleOptions('GET /api/scraps');

const PREP_COUNT = Number(__ENV.PREP_COUNT || 10);

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

let prepared = false;

export default function (data) {
    const session = getSession();

    if (!prepared) {
        for (let i = 0; i < PREP_COUNT && i < data.productIds.length; i++) {
            http.post(`${IDENTITY_BASE_URL}/api/scraps/${data.productIds[i]}`, null, {
                headers: authHeaders(session),
                tags: { name: 'PREP POST /api/scraps/:productId' },
            });
        }
        prepared = true;
    }

    const res = http.get(`${IDENTITY_BASE_URL}/api/scraps?page=0&size=10`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/scraps' },
    });

    check(res, { '스크랩 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
