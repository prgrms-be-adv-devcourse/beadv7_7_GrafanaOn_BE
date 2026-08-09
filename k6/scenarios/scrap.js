import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    IDENTITY_BASE_URL,
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import { getSession, authHeaders } from '../config/auth.js';

// Scrap 시나리오: 스크랩 추가 → 목록 조회 → 삭제 (자기정리형 사이클)
// PRODUCT_IDS 환경변수로 대상 상품을 지정한다. 없으면 setup에서 목록 조회로 채운다.

export const options = { stages, thresholds: THRESHOLDS };

export function setup() {
    if (__ENV.PRODUCT_IDS) {
        return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
    }

    // PREPARING 상품은 판매자 본인에게만 보이므로(Product.validateVisible) ON_SALE만 쓴다.
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

    // 1. 스크랩 추가
    const addRes = http.post(`${IDENTITY_BASE_URL}/api/scraps/${productId}`, null, {
        headers: authHeaders(session),
        tags: { name: 'POST /api/scraps/:productId' },
    });
    check(addRes, { '스크랩 추가 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 2. 스크랩 목록 조회
    const listRes = http.get(
        `${IDENTITY_BASE_URL}/api/scraps?page=0&size=10`,
        {
            headers: authHeaders(session),
            tags: { name: 'GET /api/scraps' },
        }
    );
    check(listRes, { '스크랩 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);

    // 3. 스크랩 삭제 (다음 반복에서 같은 상품을 다시 추가할 수 있도록 정리)
    const deleteRes = http.del(
        `${IDENTITY_BASE_URL}/api/scraps/${productId}`,
        null,
        {
            headers: authHeaders(session),
            tags: { name: 'DELETE /api/scraps/:productId' },
        }
    );
    check(deleteRes, { '스크랩 삭제 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}