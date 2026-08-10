import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    registerSeller,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: PATCH /api/products/{productId}
//
// 수정은 멱등하므로 같은 상품에 반복 호출할 수 있다.
// VU별로 상품 하나를 만들어 두고(PREP) 그것만 계속 수정한다.

export const options = singleOptions('PATCH /api/products/:id');

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
let targetId = null;

export default function () {
    const session = getSession();

    if (!sellerRegistered) {
        registerSeller(session);
        sellerRegistered = true;
    }

    if (targetId === null) {
        http.post(
            `${COMMERCE_BASE_URL}/api/products`,
            JSON.stringify(productPayload(`${RUN_ID}-upd-${__VU}`)),
            { headers: authJsonHeaders(session), tags: { name: 'PREP POST /api/products' } }
        );

        const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
            headers: authHeaders(session),
            tags: { name: 'PREP GET /api/products/me' },
        });

        const mine = myRes.json('data') || [];
        if (mine.length === 0) {
            throw new Error('수정할 상품을 만들지 못했습니다.');
        }
        targetId = mine[mine.length - 1].id;
    }

    const { saleType, ...updatePayload } = productPayload(`${RUN_ID}-upd-${__VU}-${__ITER}`);

    const res = http.patch(
        `${COMMERCE_BASE_URL}/api/products/${targetId}`,
        JSON.stringify(updatePayload),
        { headers: authJsonHeaders(session), tags: { name: 'PATCH /api/products/:id' } }
    );

    check(res, { '상품 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
