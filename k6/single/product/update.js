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

// 측정 대상: PATCH /api/products/{productId}
//
// 수정은 멱등하므로 같은 상품에 반복 호출할 수 있다.
// 대상 상품은 setup()에서 미리 만든다.
//
// ⚠️ 부하 구간 안에서 상품을 만들면 안 된다.
//    POST /api/products 역시 트랜잭션 안에서 identity를 두 번 호출하는 구조라
//    (ProductService.createProduct → validateMember, validateSeller)
//    커넥션 풀이 마르는 순간 함께 실패한다.
//    그러면 그 VU는 측정 대상 요청을 한 번도 보내지 못한 채 표본에서 빠지고,
//    남은 표본만으로 계산된 성공률·p95가 실제보다 좋게 나온다.
//    실측에서 VU 50 구간에 25건이 이렇게 빠졌다.

export const options = Object.assign(
    singleOptions('PATCH /api/products/:id'),
    // 계정 수만큼 순차로 준비하므로 기본 60초로는 부족하다.
    { setupTimeout: '5m' }
);

const RUN_ID = Date.now().toString(36).slice(-5);

// 최대 VU 수 이상이어야 한다. 모자라면 남의 상품을 수정하려다 403이 난다.
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

export function setup() {
    const targets = [];

    // getSession()은 VU 번호로 계정을 고른다(((__VU - 1) % BUYER_COUNT) + 1).
    // 소유자가 어긋나지 않도록 여기서도 1번부터 같은 순서로 만든다.
    for (let index = 1; index <= TARGET_COUNT; index++) {
        const session = loginAsBuyer(index);
        registerSeller(session);

        const createRes = http.post(
            `${COMMERCE_BASE_URL}/api/products`,
            JSON.stringify(productPayload(`${RUN_ID}-upd-${index}`)),
            { headers: authJsonHeaders(session), tags: { name: 'SETUP POST /api/products' } }
        );

        const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
            headers: authHeaders(session),
            tags: { name: 'SETUP GET /api/products/me' },
        });

        const mine = myRes.json('data') || [];
        if (mine.length === 0) {
            throw new Error(
                `${index}번 계정의 대상 상품을 만들지 못했습니다. ` +
                `생성=${createRes.status} 목록=${myRes.status} body=${createRes.body}`
            );
        }

        targets.push(mine[mine.length - 1].id);
    }

    console.log(`대상 상품 ${targets.length}개 준비 완료`);
    return { targets };
}

export default function (data) {
    const session = getSession();
    const targetId = data.targets[(__VU - 1) % data.targets.length];

    const { saleType, ...updatePayload } = productPayload(`${RUN_ID}-upd-${__VU}-${__ITER}`);

    const res = http.patch(
        `${COMMERCE_BASE_URL}/api/products/${targetId}`,
        JSON.stringify(updatePayload),
        { headers: authJsonHeaders(session), tags: { name: 'PATCH /api/products/:id' } }
    );

    check(res, { '상품 수정 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
