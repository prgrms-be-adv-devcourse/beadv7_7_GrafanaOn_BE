import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import { getSession, authHeaders, authJsonHeaders } from '../../config/auth.js';

import { SharedArray } from 'k6/data';

// 측정 대상: GET /api/search/products
//
// 인증이 필요 없는 공개 API다.
// 측정 시점(상품 30,200건) 기준 p95 3.61초로 목표(100ms)를 36배 초과했다.
// LIKE '%keyword%' 가 인덱스를 타지 못해 전체 스캔이 발생하는 것으로 추정된다.
//
// ⚠️ page는 1부터 시작한다(@Min(1)). 0을 보내면 검증 실패한다.

export const options = singleOptions('GET /api/search/products', 3000);

const keywords = new SharedArray('keywords', () => [
    '나이키', '아디다스', '신발', '가방', '자켓',
]);

const types = ['PRODUCT_NAME', 'CATEGORY', 'STORY_CONTENT'];
const sorts = ['LATEST', 'VIEW_COUNT', 'PRICE_ASC', 'PRICE_DESC'];
const categories = [
    'SNEAKERS', 'SPORTS_SHOES', 'DRESS_SHOES', 'BOOTS', 'SANDALS_SLIDES', 'WINTER_SHOES',
];

function pick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
    const type = pick(types);
    const sort = pick(sorts);
    const keyword = type === 'CATEGORY' ? pick(categories) : pick(keywords);

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/search/products` +
        `?keyword=${encodeURIComponent(keyword)}&type=${type}&sort=${sort}&page=1&size=20`,
        { tags: { name: 'GET /api/search/products' } }
    );

    check(res, { '검색 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
