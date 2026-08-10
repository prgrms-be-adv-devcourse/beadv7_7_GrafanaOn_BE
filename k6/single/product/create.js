import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    registerSeller,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/products
//
// 상품 등록 시 판매자 검증(identity-service 호출)이 포함된다.
// 그 서비스 간 호출이 응답시간에 얼마나 기여하는지 보는 것이 목적이다.
//
// ⚠️ 반복 횟수만큼 상품이 쌓인다(model_number = LT-*).
//    테스트 후 sql/cleanup-runtime.sql 로 정리할 것.

export const options = singleOptions('POST /api/products');

const RUN_ID = Date.now().toString(36).slice(-5);

function productPayload(seq) {
    return {
        saleType: 'IMMEDIATE',
        productImageContents: [
            { sortOrder: 1, url: 'https://example.com/single.jpg', story: '단일 측정용 스토리' },
        ],
        brand: '나이키',
        name: `단일측정 상품 ${seq}`,
        price: 139000,
        modelNumber: `LT-${seq}`,
        category: 'SNEAKERS',
        releaseDate: '2025-01-01',
        description: '단일 엔드포인트 측정용 상품',
    };
}

let sellerRegistered = false;

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/products`,
        JSON.stringify(productPayload(`${RUN_ID}-${__VU}-${__ITER}`)),
        { headers: authJsonHeaders(session), tags: { name: 'POST /api/products' } }
    );

    check(res, { '상품 등록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
