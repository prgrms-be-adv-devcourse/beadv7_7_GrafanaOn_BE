import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/offers/{offerId}
//
// 조회라 반복 제약이 없다. VU별로 오퍼 하나를 만들어 두고(PREP) 그것만 계속 조회한다.

export const options = singleOptions('GET /api/offers/:id');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

let offerId = null;

export default function (data) {
    const buyer = getSession();

    if (offerId === null) {
        const productId = data.productIds[(__VU - 1) % data.productIds.length];

        const snapshotRes = http.post(
            `${COMMERCE_BASE_URL}/api/offers/snapshot`,
            JSON.stringify({ productId }),
            { headers: authJsonHeaders(buyer), tags: { name: 'PREP POST /api/offers/snapshot' } }
        );

        const snapshotId = snapshotRes.json('data.snapshotId');
        if (!snapshotId) throw new Error('스냅샷 생성 실패');

        const offerRes = http.post(
            `${COMMERCE_BASE_URL}/api/offers`,
            JSON.stringify({
                snapshotId,
                title: `단일측정 오퍼 ${__VU}`,
                story: '조회 측정용 오퍼입니다.',
                delivery: '서울특별시 강남구 테헤란로 123',
            }),
            { headers: authJsonHeaders(buyer), tags: { name: 'PREP POST /api/offers' } }
        );

        offerId = offerRes.json('data.id');
        if (!offerId) throw new Error('오퍼 생성 실패');
    }

    const res = http.get(`${COMMERCE_BASE_URL}/api/offers/${offerId}`, {
        headers: authHeaders(buyer),
        tags: { name: 'GET /api/offers/:id' },
    });

    check(res, { '오퍼 상세 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
