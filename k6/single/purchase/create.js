import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getBuyerSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/purchases
//
// 구매가 성사되면 해당 상품은 판매완료가 되어 재구매할 수 없다.
// 즉 요청 1건 = IMMEDIATE 상품 1개 소모다.
//
// ⚠️ 총 반복 횟수가 보유 상품 수를 넘으면 실패가 발생한다.
//    측정 시점 기준 ON_SALE + IMMEDIATE 상품은 약 10,000건이다.
//      TEST_TYPE=focus (2분 40초) → 약 4,000건 소모   → 여유
//      기본 load (10분 10초)      → 약 12,000건 필요  → 부족
//    처음에는 focus 로 측정할 것.
//
// 지갑 잔액이 필요하므로 sql/seed-wallet.sql 로 잔액이 채워진 고정 계정을 쓴다.

export const options = singleOptions('POST /api/purchases');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error(
            '-e PRODUCT_IDS=... 가 필요합니다. ' +
            'sql/get-product-ids.sql 의 IMMEDIATE 목록을 사용하세요.'
        );
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const buyer = getBuyerSession();

    // 상품이 1회용이므로 VU끼리 겹치지 않게 고른다.
    const index = (__VU - 1) * 10007 + __ITER;
    const productId = data.productIds[index % data.productIds.length];

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/purchases`,
        JSON.stringify({ productId, delivery: '서울특별시 강남구 테헤란로 123' }),
        { headers: authJsonHeaders(buyer), tags: { name: 'POST /api/purchases' } }
    );

    check(res, { '구매 생성 201': (r) => r.status === 201 });
    sleep(THINK_TIME);
}
