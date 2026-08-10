import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/offers/products/{productId}
//
// ⚠️ 현재 실행 불가 — 판매자 로그인이 되지 않습니다.
//
//    이 API는 상품 소유자(판매자)만 호출할 수 있습니다(OF-004 NOT_OFFER_SELLER).
//    그런데 오퍼가 달린 상품의 판매자는 sql/seed-bulk.sql 이 만든 계정이고,
//    비밀번호 해시가 더미라 로그인 자체가 되지 않습니다.
//
//    실행하려면 둘 중 하나가 선행되어야 합니다.
//      1) seed.js 가 판매자를 결정적 이메일(loadtest-seller-N)로 생성하도록 변경
//      2) 로그인 가능한 판매자가 소유한 ON_SALE 상품을 별도로 준비
//
//    준비되면 SELLER_EMAIL / SELLER_PASSWORD 환경변수로 판매자 계정을 넘겨 실행합니다.
//    자세한 내용은 CAUTION.md 4번 참고.
//
// 상품에 오퍼가 쌓일수록 무거워지는 구간이라 성능상 중요하다.

export const options = singleOptions('GET /api/offers/products/:productId');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const seller = getSession(); // ⚠️ 실제로는 상품 소유자 세션이어야 한다
    const productId = data.productIds[__ITER % data.productIds.length];

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/offers/products/${productId}`,
        { headers: authHeaders(seller), tags: { name: 'GET /api/offers/products/:productId' } }
    );

    check(res, { '상품별 오퍼 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
