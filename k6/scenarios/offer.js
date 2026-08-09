import http from 'k6/http';
import { check, sleep } from 'k6';
import {
    COMMERCE_BASE_URL,
    THINK_TIME,
    THRESHOLDS,
} from '../config/environment.js';
import { stages } from '../config/stages.js';
import { getSession, authHeaders, authJsonHeaders } from '../config/auth.js';

// Offer 시나리오(구매자 흐름): 스냅샷 생성 → 오퍼 등록 → 오퍼 상세
//
// ⚠️ 대상 상품은 반드시 status=ON_SALE 이어야 한다.
//    PREPARING 상품에 스냅샷을 만들면 OF-008(상품 정보를 조회하지 못했습니다)로 실패한다.
//    그런데 POST /api/products로 만든 상품은 PREPARING으로 생성되고, 이를 ON_SALE로
//    바꾸는 API가 없다(changeStatusToOnSale()이 프로덕션 코드에서 호출되지 않음).
//    따라서 이 시나리오는 SQL로 ON_SALE 처리된 상품이 있어야 돌아간다.
//    → sql/seed-bulk.sql 또는 sql/activate-products.sql 실행 필요
//
// 제외한 엔드포인트
//  - GET /api/offers/products/{id} : 상품 소유자(판매자)만 호출 가능(OF-004).
//    구매자 흐름에 넣을 수 없고, 판매자 계정으로 상품을 만들어도 PREPARING이라 쓸 수 없다.
//  - PATCH /api/offers/{id}/accept, /reject : 1회성이라 반복 불가, 상품이 소모된다.
//  자세한 내용은 CAUTION.md 참고.

export const options = { stages, thresholds: THRESHOLDS };

export function setup() {
    if (__ENV.PRODUCT_IDS) {
        return { productIds: __ENV.PRODUCT_IDS.split(',').map(Number) };
    }

    // 오퍼 대상은 saleType=OFFER 이면서 status=ON_SALE 인 상품이어야 한다.
    // ⚠️ GET /api/products는 페이지네이션이 없어 ON_SALE 전체 조회 시
    //    4.5MB / 약 29초가 걸린다. 테스트마다 이 비용을 치르지 않으려면
    //    sql/get-product-ids.sql로 ID를 미리 뽑아 -e PRODUCT_IDS=... 로 넘길 것.
    console.warn(
        'PRODUCT_IDS가 지정되지 않아 GET /api/products로 조회합니다. ' +
        '응답이 커서 수십 초 걸릴 수 있습니다. sql/get-product-ids.sql 사용을 권장합니다.'
    );

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/products?saleType=OFFER&status=ON_SALE`
    );
    const products = res.json('data') || [];
    const productIds = products
        .filter((p) => p.saleType === 'OFFER' && p.status === 'ON_SALE')
        .map((p) => p.id)
        .filter(Boolean);

    if (productIds.length === 0) {
        throw new Error(
            '판매중(ON_SALE)인 OFFER 상품이 없습니다. ' +
            'sql/seed-bulk.sql 또는 sql/activate-products.sql을 먼저 실행하세요.'
        );
    }

    console.log(`오퍼 대상 ON_SALE 상품 ${productIds.length}개 확보`);
    return { productIds };
}

export default function (data) {
    const buyer = getSession();

    // 상품을 무작위가 아니라 순차로 고른다.
    //
    // 한 구매자는 한 상품에 오퍼를 한 번만 넣을 수 있다.
    // 스냅샷이 (writerId, productId)로 재사용되는데, 이미 오퍼에 연결된 스냅샷은
    // 다시 쓸 수 없어 OFS-001("이미 오퍼에 연결된 스냅샷입니다")로 실패한다.
    // VU별 계정은 고정이므로, 무작위로 고르면 같은 상품을 다시 집어 실패한다.
    //
    // ⚠️ 따라서 VU당 반복 횟수가 ON_SALE OFFER 상품 수를 넘으면 실패가 발생한다.
    //    seed-bulk.sql 기본값(상품 30,000 중 OFFER 절반)이면 충분하다.
    const index = (__VU - 1) * 17 + __ITER;
    const productId = data.productIds[index % data.productIds.length];

    // 1. 오퍼 스냅샷 생성
    const snapshotRes = http.post(
        `${COMMERCE_BASE_URL}/api/offers/snapshot`,
        JSON.stringify({ productId }),
        {
            headers: authJsonHeaders(buyer),
            tags: { name: 'POST /api/offers/snapshot' },
        }
    );
    check(snapshotRes, { '스냅샷 생성 201': (r) => r.status === 201 });

    // CreateOfferSnapshotResponse: { snapshotId, productId, modelNumberSnapshot, priceSnapshot }
    const snapshotId = snapshotRes.json('data.snapshotId');
    if (!snapshotId) return;
    sleep(THINK_TIME);

    // 2. 오퍼 등록
    const offerRes = http.post(
        `${COMMERCE_BASE_URL}/api/offers`,
        JSON.stringify({
            snapshotId,
            title: `부하테스트 오퍼 ${__VU}-${__ITER}`,
            story: '부하테스트용 오퍼 스토리입니다.',
            delivery: '서울특별시 강남구 테헤란로 123',
        }),
        {
            headers: authJsonHeaders(buyer),
            tags: { name: 'POST /api/offers' },
        }
    );
    check(offerRes, { '오퍼 등록 201': (r) => r.status === 201 });

    // OfferResponse의 식별자 필드명은 offerId가 아니라 id다.
    const offerId = offerRes.json('data.id');
    if (!offerId) return;
    sleep(THINK_TIME);

    // 3. 오퍼 상세 조회 (자기 오퍼)
    const detailRes = http.get(`${COMMERCE_BASE_URL}/api/offers/${offerId}`, {
        headers: authHeaders(buyer),
        tags: { name: 'GET /api/offers/:id' },
    });
    check(detailRes, { '오퍼 상세 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
