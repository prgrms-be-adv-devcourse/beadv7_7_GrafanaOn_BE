import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    loginAsBuyer,
    registerSeller,
    authHeaders,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: DELETE /api/products/{productId}
//
// 삭제는 상품당 1회뿐이라 매 반복 새로 만들고 삭제한다. 생성/삭제가 짝을 이뤄 데이터가 남지 않는다.
//
// ⚠️ 이 스크립트만 준비 작업을 setup()으로 옮기지 못한다.
//    요청 1건이 상품 1개를 소모하므로 focus 기준 약 4,000개가 필요한데,
//    그만큼을 미리 만들면 준비에만 수 분이 걸리고 정리 대상도 그만큼 늘어난다.
//    (update.js는 대상을 재사용할 수 있어 setup()으로 옮겼다)
//
//    대신 선행 단계 성공률을 체크로 남긴다.
//    POST /api/products 도 트랜잭션 안에서 identity를 두 번 호출하므로
//    커넥션 풀이 마르면 함께 실패하는데, 그때 조용히 넘어가면
//    측정 대상 요청이 기록되지 않아 성공률이 실제보다 좋게 나온다.
//    '[선행] 상품 생성 200' 이 100%가 아니면 그만큼 표본에서 빠졌다는 뜻이다.

export const options = Object.assign(
    singleOptions('DELETE /api/products/:id'),
    { setupTimeout: '5m' }
);

const RUN_ID = Date.now().toString(36).slice(-5);

// 최대 VU 수 이상이어야 한다.
const TARGET_COUNT = Number(__ENV.TARGET_COUNT || 50);

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

// 판매자 등록은 반복마다 할 필요가 없으므로 부하 전에 끝내둔다.
export function setup() {
    for (let index = 1; index <= TARGET_COUNT; index++) {
        registerSeller(loginAsBuyer(index));
    }

    console.log(`계정 ${TARGET_COUNT}개 판매자 상태 확보 완료`);
    return {};
}

export default function () {
    const session = getSession();

    const createRes = http.post(
        `${COMMERCE_BASE_URL}/api/products`,
        JSON.stringify(productPayload(`${RUN_ID}-del-${__VU}-${__ITER}`)),
        { headers: authJsonHeaders(session), tags: { name: 'PREP POST /api/products' } }
    );
    check(createRes, { '[선행] 상품 생성 200': (r) => r.status === 200 });

    const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
        headers: authHeaders(session),
        tags: { name: 'PREP GET /api/products/me' },
    });
    check(myRes, { '[선행] 내 상품 조회 200': (r) => r.status === 200 });

    const mine = myRes.json('data') || [];
    if (mine.length === 0) {
        // 선행이 실패해도 즉시 다음 반복으로 가지 않는다.
        // sleep 없이 반환하면 실패한 VU가 초당 수백 건으로 서버를 때린다.
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
