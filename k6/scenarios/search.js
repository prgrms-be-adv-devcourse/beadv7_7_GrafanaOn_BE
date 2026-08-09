import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';

// Search 시나리오: 공개 API(GET /api/search/products), 인증 불필요.
// 검색어/타입/정렬을 매 반복 무작위로 섞어 캐시 효과를 줄인다.

export const options = { stages, thresholds: THRESHOLDS };

// seed.js가 상품명과 스토리에 이 키워드들을 섞어 넣으므로 실제로 결과가 잡힌다.
const keywords = new SharedArray('keywords', () => [
    '나이키',
    '아디다스',
    '신발',
    '가방',
    '자켓',
]);

const types = ['PRODUCT_NAME', 'CATEGORY', 'STORY_CONTENT'];
const sorts = ['LATEST', 'VIEW_COUNT', 'PRICE_ASC', 'PRICE_DESC'];
const categories = [
    'SNEAKERS',
    'SPORTS_SHOES',
    'DRESS_SHOES',
    'BOOTS',
    'SANDALS_SLIDES',
    'WINTER_SHOES',
];

function pick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
    const type = pick(types);
    const sort = pick(sorts);

    // CATEGORY 검색은 키워드가 카테고리 Enum 값이어야 결과가 나온다.
    const keyword = type === 'CATEGORY' ? pick(categories) : pick(keywords);

    // ⚠️ 검색 API의 page는 1부터 시작한다(@Min(1)). 0을 보내면 검증 실패한다.
    //    스크랩(GET /api/scraps)은 0부터라 규칙이 다르니 주의.
    const res = http.get(
        `${COMMERCE_BASE_URL}/api/search/products` +
        `?keyword=${encodeURIComponent(keyword)}&type=${type}&sort=${sort}&page=1&size=20`,
        { tags: { name: 'GET /api/search/products' } }
    );

    check(res, { '검색 200': (r) => r.status === 200 });

    sleep(THINK_TIME);
}
