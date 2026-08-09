import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
    JSON_HEADERS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import { getSession, authHeaders, authJsonHeaders } from '../config/auth.js';

// Cart 시나리오: 담기 → 조회 → 선택 삭제 → 전체 비우기 (자기정리형)
// CartItem에 유니크 제약이 있어 같은 상품 중복 담기는 실패하므로, 매 반복 끝에 비운다.

export const options = { stages, thresholds: THRESHOLDS };

export function setup() {
    if (__ENV.PRODUCT_IDS) {
        return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
    }
    // PREPARING 상품은 판매자 본인에게만 보인다(Product.validateVisible).
    // 담기는 되더라도 장바구니 조회 시 상품 정보를 못 가져와 실패하므로 ON_SALE만 쓴다.
    // ⚠️ GET /api/products는 페이지네이션이 없어 ON_SALE 전체 조회 시
    //    4.5MB / 약 29초가 걸린다. 테스트마다 이 비용을 치르지 않으려면
    //    sql/get-product-ids.sql로 ID를 미리 뽑아 -e PRODUCT_IDS=... 로 넘길 것.
    console.warn(
        'PRODUCT_IDS가 지정되지 않아 GET /api/products로 조회합니다. ' +
        '응답이 커서 수십 초 걸릴 수 있습니다. sql/get-product-ids.sql 사용을 권장합니다.'
    );

    const res = http.get(`${COMMERCE_BASE_URL}/api/products?status=ON_SALE`);
    const products = res.json('data') || [];
    const productIds = products
        .filter((p) => p.status === 'ON_SALE')
        .map((p) => p.id)
        .filter(Boolean);

    if (productIds.length === 0) {
        throw new Error(
            '판매중(ON_SALE)인 상품이 없습니다. sql/seed-bulk.sql 또는 ' +
            'sql/activate-products.sql을 먼저 실행하거나 -e PRODUCT_IDS=1,2,3 으로 지정하세요.'
        );
    }
    return { productIds };
}

export default function (data) {
    const session = getSession();
    const productId =
        data.productIds[Math.floor(Math.random() * data.productIds.length)];

    // 1. 장바구니 담기
    const addRes = http.post(
        `${COMMERCE_BASE_URL}/api/carts/items`,
        JSON.stringify({ productId }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'POST /api/carts/items' },
        }
    );
    check(addRes, { '장바구니 담기 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 2. 장바구니 조회 (상품 정보 매핑이 들어가는 구간 — 병목 주시 대상)
    const listRes = http.get(`${COMMERCE_BASE_URL}/api/carts`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/carts' },
    });
    check(listRes, { '장바구니 조회 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 3. 선택 삭제
    const deleteSelectedRes = http.del(
        `${COMMERCE_BASE_URL}/api/carts/items?productIds=${productId}`,
        null,
        {
            headers: authHeaders(session),
            tags: { name: 'DELETE /api/carts/items' },
        }
    );
    check(deleteSelectedRes, { '선택 삭제 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 4. 전체 비우기 (다음 반복을 위한 정리)
    const deleteAllRes = http.del(
        `${COMMERCE_BASE_URL}/api/carts/items/all`,
        null,
        {
            headers: authHeaders(session),
            tags: { name: 'DELETE /api/carts/items/all' },
        }
    );
    check(deleteAllRes, { '전체 비우기 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}