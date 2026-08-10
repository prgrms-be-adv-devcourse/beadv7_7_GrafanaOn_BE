import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages, burstStages } from '../config/stages.js';
import {
    getSession,
    getBuyerSession,
    authHeaders,
    authJsonHeaders,
} from '../config/auth.js';

// Purchase 시나리오
//  - MODE=read (기본): 내 구매 목록 + 구매 상세. 반복 실행에 안전하다.
//  - MODE=write      : 즉시구매 생성 → 상세 → 취소.
//
// ⚠️ write 모드 제약
//  1) 구매가 성사되면 해당 상품은 판매완료가 되어 재구매할 수 없다.
//     즉 반복 1회당 IMMEDIATE 상품 1개가 소모된다.
//     그래서 write 모드는 load/stress 단계가 아니라 burst(VU 10 / 30초 ≈ 90건)로
//     자동 전환된다. 상품을 넉넉히 시딩한 뒤 단발성으로만 측정한다.
//  2) 지갑 잔액이 필요한데 충전 API가 없다(결제 confirm은 실제 토스 호출이라 사용 금지).
//     seed.js가 만든 고정 계정에 sql/seed-wallet.sql로 잔액을 넣고, 그 계정으로 로그인한다.

const MODE = __ENV.MODE || 'read';

export const options = {
    stages: MODE === 'write' ? burstStages : stages,
    thresholds: THRESHOLDS,
};

export function setup() {
    if (MODE !== 'write') return {};

    if (__ENV.PRODUCT_IDS) {
        return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
    }

    // 판매완료된 상품은 재구매가 불가능하므로 status까지 걸러야 한다.
    // ⚠️ GET /api/products는 페이지네이션이 없어 ON_SALE 전체 조회 시
    //    4.5MB / 약 29초가 걸린다. 테스트마다 이 비용을 치르지 않으려면
    //    sql/get-product-ids.sql로 ID를 미리 뽑아 -e PRODUCT_IDS=... 로 넘길 것.
    console.warn(
        'PRODUCT_IDS가 지정되지 않아 GET /api/products로 조회합니다. ' +
        '응답이 커서 수십 초 걸릴 수 있습니다. sql/get-product-ids.sql 사용을 권장합니다.'
    );

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/products?saleType=IMMEDIATE&status=ON_SALE`
    );
    const products = res.json('data') || [];
    const productIds = products
        .filter((p) => p.saleType === 'IMMEDIATE' && p.status === 'ON_SALE')
        .map((p) => p.id)
        .filter(Boolean);

    if (productIds.length === 0) {
        throw new Error(
            '구매 가능한 IMMEDIATE 상품이 없습니다. seed.js를 먼저 실행하거나 ' +
            '-e PRODUCT_IDS=1,2,3 으로 지정하세요.'
        );
    }

    console.log(
        `구매 대상 상품 ${productIds.length}개. ` +
        'burst 단계는 약 90건을 소모하므로 부족하면 seed.js를 다시 실행하세요.'
    );

    return { productIds };
}

export default function (data) {
    // read 모드는 아무 계정이나 상관없지만,
    // write 모드는 지갑 잔액이 시딩된 고정 계정이어야 한다.
    const session = MODE === 'write' ? getBuyerSession() : getSession();

    if (MODE === 'read') {
        // 1. 내 구매 목록 조회
        const myRes = http.get(`${COMMERCE_BASE_URL}/api/purchases/me`, {
            headers: authHeaders(session),
            tags: { name: 'GET /api/purchases/me' },
        });
        check(myRes, { '내 구매 목록 200': (r) => r.status === 200 });
        sleep(THINK_TIME);

        // 2. 구매 상세 조회 (목록에 항목이 있을 때만)
        const purchases = myRes.json('data') || [];
        if (purchases.length > 0) {
            // PurchaseResponse의 식별자 필드명은 purchaseId가 아니라 id다.
            const purchaseId = purchases[0].id;
            const detailRes = http.get(
                `${COMMERCE_BASE_URL}/api/purchases/${purchaseId}`,
                {
                    headers: authHeaders(session),
                    tags: { name: 'GET /api/purchases/:id' },
                }
            );
            check(detailRes, { '구매 상세 200': (r) => r.status === 200 });
            sleep(THINK_TIME);
        }
        return;
    }

    // ---- MODE=write ----
    // 상품이 1회용이므로, VU/반복 조합으로 서로 다른 상품을 집어 중복 구매를 줄인다.
    const index = (__VU - 1) * 1000 + __ITER;
    const productId = data.productIds[index % data.productIds.length];

    // 1. 즉시구매 생성
    const createRes = http.post(
        `${COMMERCE_BASE_URL}/api/purchases`,
        JSON.stringify({
            productId,
            delivery: '서울특별시 강남구 테헤란로 123',
        }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'POST /api/purchases' },
        }
    );
    check(createRes, { '구매 생성 201': (r) => r.status === 201 });

    // PurchaseResponse: { id, number, status, productId, amount, purchasedAt, paymentDueAt, delivery }
    const purchaseId = createRes.json('data.id');
    if (!purchaseId) {
        // 선행 요청이 실패해도 즉시 다음 반복으로 가지 않는다.
    // sleep 없이 반환하면 실패한 VU가 초당 수백 건으로 폭주한다.
        sleep(THINK_TIME);
        return;
    }
    sleep(THINK_TIME);

    // 2. 구매 상세 조회
    const detailRes = http.get(
        `${COMMERCE_BASE_URL}/api/purchases/${purchaseId}`,
        {
            headers: authHeaders(session),
            tags: { name: 'GET /api/purchases/:id' },
        }
    );
    check(detailRes, { '구매 상세 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 3. 구매 취소
    const cancelRes = http.del(
        `${COMMERCE_BASE_URL}/api/purchases/${purchaseId}/cancel`,
        null,
        {
            headers: authHeaders(session),
            tags: { name: 'DELETE /api/purchases/:id/cancel' },
        }
    );
    check(cancelRes, { '구매 취소 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
