import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: PATCH /api/offers/{offerId}/reject
//
// ⚠️ 현재 실행 불가 — 판매자 로그인이 되지 않습니다.
//
//    이 API는 상품 소유자(판매자)만 호출할 수 있습니다(OF-004 NOT_OFFER_SELLER).
//    그런데 오퍼가 달린 상품의 판매자는 sql/seed-bulk.sql 이 만든 계정이고,
//    비밀번호 해시가 더미라 로그인 자체가 되지 않습니다.
//
//    실행하려면 둘 중 하나가 선행되어야 합니다.
//      1) seed.js 가 판매자를 결정적 이메일(loadtest-seller-N)로 생성하도록 변경
//      2) 로그인 가능한 판매자가 소유한 ON_SALE 상품을 별도로 준비
//
//    준비되면 SELLER_EMAIL / SELLER_PASSWORD 환경변수로 판매자 계정을 넘겨 실행합니다.
//    자세한 내용은 CAUTION.md 4번 참고.
//
// ⚠️ 추가 제약: 오퍼 1건당 1회만 가능하다.
//    반복 횟수만큼 오퍼가 소모되므로 사전에 대량 생성이 필요하다.
//    거절은 상품을 소모하지 않는다.
//
//    OFFER_IDS 환경변수로 처리할 오퍼 목록을 넘긴다.

export const options = singleOptions('PATCH /api/offers/:id/reject');

export function setup() {
    if (!__ENV.OFFER_IDS) {
        throw new Error(
            '-e OFFER_IDS=... 가 필요합니다. ' +
            '판매자 계정 준비가 선행되어야 합니다(CAUTION.md 4번 참고).'
        );
    }
    return { offerIds: __ENV.OFFER_IDS.split(',').map(Number) };
}

export default function (data) {
    const seller = getSession(); // ⚠️ 실제로는 상품 소유자 세션이어야 한다

    const index = (__VU - 1) * 10007 + __ITER;
    const offerId = data.offerIds[index % data.offerIds.length];

    const res = http.patch(
        `${COMMERCE_BASE_URL}/api/offers/${offerId}/reject`,
        null,
        { headers: authHeaders(seller), tags: { name: 'PATCH /api/offers/:id/reject' } }
    );

    check(res, { '오퍼 거절 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
