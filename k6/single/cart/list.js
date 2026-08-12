import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    loginAsBuyer,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/carts
//
// 장바구니 항목마다 상품 정보를 조회해 붙이므로 N+1 발생 여부를 보는 것이 목적이다.
// 항목 수가 많을수록 드러나므로 계정별로 여러 건 담아둔다.
//
// ⚠️ 담는 작업은 setup()에서 한다.
//    부하 구간에서 담으면 (1) 준비 요청이 측정 구간의 부하에 섞이고
//    (2) 담기가 실패한 VU는 조회를 못 해 표본에서 빠진다.
//
// ⚠️ 시작할 때 장바구니를 비운다.
//    cleanup SQL이 고정 계정(loadtest-buyer-N)의 cart_item을 지우지 않던 시기가 있어
//    이전 실행분이 남아 있을 수 있다. 항목 수가 달라지면 실행 간 비교가 무의미해진다.

export const options = Object.assign(
    singleOptions('GET /api/carts'),
    { setupTimeout: '5m' }
);

const PREP_COUNT = Number(__ENV.PREP_COUNT || 15);

// 최대 VU 수 이상이어야 한다. 모자라면 준비되지 않은 계정이 조회에 참여한다.
const TARGET_COUNT = Number(__ENV.TARGET_COUNT || 50);

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }

    const productIds = __ENV.PRODUCT_IDS.split(',').map(Number);
    const itemCount = Math.min(PREP_COUNT, productIds.length);

    for (let index = 1; index <= TARGET_COUNT; index++) {
        const session = loginAsBuyer(index);

        http.del(`${COMMERCE_BASE_URL}/api/carts/items/all`, null, {
            headers: authHeaders(session),
            tags: { name: 'SETUP DELETE /api/carts/items/all' },
        });

        for (let i = 0; i < itemCount; i++) {
            const res = http.post(
                `${COMMERCE_BASE_URL}/api/carts/items`,
                JSON.stringify({ productId: productIds[i] }),
                { headers: authJsonHeaders(session), tags: { name: 'SETUP POST /api/carts/items' } }
            );

            if (res.status !== 200) {
                throw new Error(
                    `${index}번 계정의 장바구니 준비 실패. ` +
                    `상품=${productIds[i]} 상태=${res.status} body=${res.body}`
                );
            }
        }
    }

    console.log(`계정 ${TARGET_COUNT}개에 항목 ${itemCount}건씩 준비 완료`);
    return { itemCount };
}

export default function () {
    const session = getSession();

    const res = http.get(`${COMMERCE_BASE_URL}/api/carts`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/carts' },
    });

    check(res, { '장바구니 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
