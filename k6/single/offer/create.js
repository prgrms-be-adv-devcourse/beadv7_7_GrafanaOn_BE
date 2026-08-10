import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/offers
//
// 한 구매자는 한 상품에 오퍼를 1회만 넣을 수 있다.
// 이미 오퍼에 연결된 스냅샷은 재사용할 수 없어 OFS-001 로 실패하므로,
// 매 반복 서로 다른 상품을 순차로 골라 스냅샷을 새로 만든다(PREP).
//
// ⚠️ VU당 반복 횟수가 대상 상품 수를 넘으면 실패가 발생한다.
//    OFFER + ON_SALE 상품을 넉넉히 넘길 것(현재 약 15,000건 보유).

export const options = singleOptions('POST /api/offers');

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }
    return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
}

export default function (data) {
    const buyer = getSession();
    const index = (__VU - 1) * 10007 + __ITER; // VU끼리 구간이 겹치지 않도록 큰 소수를 곱한다
    const productId = data.productIds[index % data.productIds.length];

    const snapshotRes = http.post(
        `${COMMERCE_BASE_URL}/api/offers/snapshot`,
        JSON.stringify({ productId }),
        { headers: authJsonHeaders(buyer), tags: { name: 'PREP POST /api/offers/snapshot' } }
    );

    check(snapshotRes, { '[선행] 스냅샷 생성 201': (r) => r.status === 201 });

    const snapshotId = snapshotRes.json('data.snapshotId');
    if (!snapshotId) {
        // 선행 요청이 실패해도 즉시 다음 반복으로 가지 않는다.
        // sleep 없이 반환하면 실패한 VU가 초당 수백 건으로 폭주한다.
        sleep(THINK_TIME);
        return;
    }

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/offers`,
        JSON.stringify({
            snapshotId,
            title: `단일측정 오퍼 ${__VU}-${__ITER}`,
            story: '단일 엔드포인트 측정용 오퍼입니다.',
            delivery: '서울특별시 강남구 테헤란로 123',
        }),
        { headers: authJsonHeaders(buyer), tags: { name: 'POST /api/offers' } }
    );

    check(res, { '오퍼 등록 201': (r) => r.status === 201 });
    sleep(THINK_TIME);
}
