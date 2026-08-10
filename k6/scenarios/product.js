import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import { getSession, authHeaders, authJsonHeaders, registerSeller } from '../config/auth.js';

// Product 시나리오
//  - MODE=read (기본): 목록 조회 + 상세 조회. 실제 트래픽 대부분을 차지하는 조회 경로.
//  - MODE=write      : 등록 → 상세 → 수정 → 삭제 라이프사이클. 삭제로 자기정리된다.
// 쓰기 모드는 DB에 상품이 쌓였다 지워지므로, 조회 모드와 분리해서 측정하는 편이 해석이 쉽다.

export const options = { stages, thresholds: THRESHOLDS };

const MODE = __ENV.MODE || 'read';

const CATEGORIES = ['SNEAKERS', 'SPORTS_SHOES', 'DRESS_SHOES', 'BOOTS', 'SANDALS_SLIDES', 'WINTER_SHOES'];
const SALE_TYPES = ['IMMEDIATE', 'OFFER'];

function pick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function productPayload() {
    return {
        saleType: pick(SALE_TYPES),
        productImageContents: [
            {
                sortOrder: 1,
                url: 'https://example.com/loadtest.jpg',
                story: '부하테스트용 스토리 내용입니다.',
            },
        ],
        brand: '나이키',
        name: `부하테스트 상품 ${__VU}-${__ITER}`,
        price: 139000,
        modelNumber: `LT-${__VU}-${__ITER}`,
        category: pick(CATEGORIES),
        releaseDate: '2025-01-01',
        description: '부하테스트용 상품 설명',
    };
}

export function setup() {
    if (MODE !== 'read') return {};

    // 조회 모드는 상세 조회 대상 ID가 필요하다.
    if (__ENV.PRODUCT_IDS) {
        return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
    }
    // PREPARING 상품은 판매자 본인에게만 보인다(Product.validateVisible).
    // 남의 PREPARING 상품을 상세 조회하면 실패하므로 ON_SALE만 대상으로 삼는다.
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

let sellerRegistered = false;

export default function (data) {
    const session = getSession();

    if (MODE === 'read') {
        // 1. 상품 목록 조회 (공개 API, 필터 포함)
        const listRes = http.get(
            `${COMMERCE_BASE_URL}/api/products?category=${pick(CATEGORIES)}`,
            { tags: { name: 'GET /api/products' } }
        );
        check(listRes, { '상품 목록 200': (r) => r.status === 200 });
        sleep(THINK_TIME);

        // 2. 상품 상세 조회 (인증 필요)
        const productId =
            data.productIds[Math.floor(Math.random() * data.productIds.length)];
        const detailRes = http.get(
            `${COMMERCE_BASE_URL}/api/products/${productId}`,
            {
                headers: authHeaders(session),
                tags: { name: 'GET /api/products/:id' },
            }
        );
        check(detailRes, { '상품 상세 200': (r) => r.status === 200 });
        sleep(THINK_TIME);
        return;
    }

    // ---- MODE=write ----
    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    // 1. 상품 등록
    const createRes = http.post(
        `${COMMERCE_BASE_URL}/api/products`,
        JSON.stringify(productPayload()),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'POST /api/products' },
        }
    );
    check(createRes, { '상품 등록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 2. 내 판매 상품 목록 (방금 등록한 상품의 id를 확보)
    const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
        headers: authHeaders(session),
        tags: { name: 'GET /api/products/me' },
    });
    check(myRes, { '내 상품 목록 200': (r) => r.status === 200 });

    const myProducts = myRes.json('data') || [];
    if (myProducts.length === 0) {
        // 선행 요청이 실패해도 즉시 다음 반복으로 가지 않는다.
    // sleep 없이 반환하면 실패한 VU가 초당 수백 건으로 폭주한다.
        sleep(THINK_TIME);
        return;
    }

    const targetId = myProducts[myProducts.length - 1].id;
    sleep(THINK_TIME);

    // 3. 상품 수정
    const { saleType, ...updatePayload } = productPayload();
    const updateRes = http.patch(
        `${COMMERCE_BASE_URL}/api/products/${targetId}`,
        JSON.stringify(updatePayload),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'PATCH /api/products/:id' },
        }
    );
    check(updateRes, { '상품 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 4. 상품 삭제 (테스트 데이터 자기정리)
    const deleteRes = http.del(
        `${COMMERCE_BASE_URL}/api/products/${targetId}`,
        null,
        {
            headers: authHeaders(session),
            tags: { name: 'DELETE /api/products/:id' },
        }
    );
    check(deleteRes, { '상품 삭제 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}