import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    signUpAndLogin,
    registerSeller,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/products
//
// ⚠️ 이 API는 페이지네이션이 없어 조건에 맞는 상품을 전부 한 응답에 담는다.
//    측정 시점(상품 30,200건) 기준 카테고리 필터 하나에 968KB / 6초가 걸렸다.
//    10분 측정 시 수 GB 아웃바운드가 발생하므로 먼저 짧게 확인할 것.
//    (TEST_TYPE=focus 로 2분 40초 측정 권장)

export const options = singleOptions('GET /api/products', 6000);

const CATEGORIES = ['SNEAKERS', 'SPORTS_SHOES', 'DRESS_SHOES', 'BOOTS', 'SANDALS_SLIDES', 'WINTER_SHOES'];

function pick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
    const res = http.get(
        `${COMMERCE_BASE_URL}/api/products?category=${pick(CATEGORIES)}&status=ON_SALE`,
        { tags: { name: 'GET /api/products' } }
    );

    check(res, { '상품 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
