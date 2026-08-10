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

// 측정 대상: DELETE /api/products/{productId}
//
// 삭제는 상품당 1회뿐이므로 매 반복 새로 만들고(PREP) 삭제를 측정한다.
// 생성/삭제가 짝을 이뤄 데이터가 남지 않는다.

export const options = singleOptions('DELETE /api/products/:id');

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

    http.post(
        `${COMMERCE_BASE_URL}/api/products`,
        JSON.stringify(productPayload(`${RUN_ID}-del-${__VU}-${__ITER}`)),
        { headers: authJsonHeaders(session), tags: { name: 'PREP POST /api/products' } }
    );

    const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
        headers: authHeaders(session),
        tags: { name: 'PREP GET /api/products/me' },
    });

    const mine = myRes.json('data') || [];
    if (mine.length === 0) {
        sleep(THINK_TIME);
        return;
    }

    const targetId = mine[mine.length - 1].id;

    const res = http.del(`${COMMERCE_BASE_URL}/api/products/${targetId}`, null, {
        headers: authHeaders(session),
        tags: { name: 'DELETE /api/products/:id' },
    });

    check(res, { '상품 삭제 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
