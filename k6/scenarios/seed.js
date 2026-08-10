import http from 'k6/http';
import { check } from 'k6';
import {
    COMMERCE_BASE_URL,
    IDENTITY_BASE_URL,
    JSON_HEADERS,
} from '../config/environment.js';
import {
    signUpAndLogin,
    authJsonHeaders,
    registerSeller,
    buyerEmail,
    BUYER_PASSWORD,
} from '../config/auth.js';

// 부하테스트 기초 데이터 생성 스크립트.
// 성능 측정이 목적이 아니므로 VU 1 / 반복 1회로 순차 실행하고 thresholds도 걸지 않는다.
//
// 생성되는 것
//   1. 판매자 계정 + 상품 (IMMEDIATE / OFFER 번갈아)
//   2. 구매 테스트용 고정 계정 (loadtest-buyer-N@example.com)
//
// 실행 후 출력되는 값을 다른 시나리오에 -e PRODUCT_IDS=... 로 넘기면 된다.
// (넘기지 않으면 각 시나리오가 GET /api/products로 자동 조회한다)

export const options = {
    vus: 1,
    iterations: 1,
};

// 기본값 근거: 구매(write)는 burst 단계(VU 10 / 30초 ≈ 90건)로 돌리고
// 구매 1건당 IMMEDIATE 상품 1개가 소모되므로, IMMEDIATE 100개를 확보한다.
const SELLER_COUNT = Number(__ENV.SELLER_COUNT || 5);
const PRODUCTS_PER_SELLER = Number(__ENV.PRODUCTS_PER_SELLER || 40);

// 고정 계정 수. VU 번호와 1:1로 매핑되므로 최대 VU 수 이상이어야 한다.
//
// 부하 테스트 중에는 회원가입을 하지 않고 이 계정들로 로그인만 한다.
// 회원가입이 commerce-service의 지갑 생성 API를 호출하는 구조라,
// 측정 중 가입을 시도하면 서비스 간 호출이 섞여 결과가 오염되기 때문이다.
//
// 200 = stress 단계의 최대 VU. 이 값이면 focus/load/stress 전부 커버된다.
const BUYER_COUNT = Number(__ENV.BUYER_COUNT || 200);

const CATEGORIES = [
    'SNEAKERS',
    'SPORTS_SHOES',
    'DRESS_SHOES',
    'BOOTS',
    'SANDALS_SLIDES',
    'WINTER_SHOES',
];

const BRANDS = ['나이키', '아디다스', '뉴발란스', '컨버스', '반스'];

// search.js가 쓰는 검색어. 상품명과 스토리에 섞어 넣어 실제로 검색 결과가 잡히게 한다.
const KEYWORDS = ['나이키', '아디다스', '신발', '가방', '자켓'];

function pick(arr, i) {
    return arr[i % arr.length];
}

function productPayload(sellerIndex, productIndex, saleType) {
    const seq = sellerIndex * PRODUCTS_PER_SELLER + productIndex;

    return {
        saleType,
        productImageContents: [
            {
                sortOrder: 1,
                url: 'https://example.com/seed.jpg',
                story: `${pick(KEYWORDS, seq)} 관련 스토리입니다. 빈티지샵에서 발굴했습니다.`,
            },
        ],
        brand: pick(BRANDS, seq),
        name: `${pick(KEYWORDS, seq)} 시드상품 ${seq}`,
        price: 50000 + (seq % 20) * 10000,
        modelNumber: `SEED-${seq}`,
        category: pick(CATEGORIES, seq),
        releaseDate: '2025-01-01',
        description: `부하테스트 시드 상품 ${seq}`,
    };
}

export default function () {
    const immediateIds = [];
    const offerIds = [];

    // ---- 1. 판매자 + 상품 ----
    for (let s = 0; s < SELLER_COUNT; s++) {
        const seller = signUpAndLogin();
        const sellerRes = registerSeller(seller);

        if (sellerRes.status !== 200) {
            console.error(
                `판매자 등록 실패 (seller ${s}): ${sellerRes.status} ${sellerRes.body}`
            );
            continue;
        }

        for (let p = 0; p < PRODUCTS_PER_SELLER; p++) {
            // IMMEDIATE / OFFER를 번갈아 생성한다.
            const saleType = p % 2 === 0 ? 'IMMEDIATE' : 'OFFER';

            const res = http.post(
                `${COMMERCE_BASE_URL}/api/products`,
                JSON.stringify(productPayload(s, p, saleType)),
                {
                    headers: authJsonHeaders(seller),
                    tags: { name: 'SEED POST /api/products' },
                }
            );

            check(res, { '시드 상품 등록 성공': (r) => r.status === 200 });

            if (res.status !== 200) {
                console.error(`상품 등록 실패: ${res.status} ${res.body}`);
            }
        }

        // 방금 등록한 상품 ID를 saleType별로 수집한다.
        const myRes = http.get(`${COMMERCE_BASE_URL}/api/products/me`, {
            headers: authJsonHeaders(seller),
            tags: { name: 'SEED GET /api/products/me' },
        });

        const myProducts = myRes.json('data') || [];
        for (const product of myProducts) {
            if (product.saleType === 'OFFER') {
                offerIds.push(product.id);
            } else {
                immediateIds.push(product.id);
            }
        }

        console.log(
            `판매자 ${s + 1}/${SELLER_COUNT} 완료 ` +
            `(memberId=${seller.memberId}, 상품 ${myProducts.length}개)`
        );
    }

    // ---- 2. 구매(write) 테스트용 고정 계정 ----
    // VU 번호와 1:1 대응되는 결정적 이메일로 만들어, SQL로 지갑 잔액을 채울 수 있게 한다.
    const buyerMemberIds = [];

    for (let b = 1; b <= BUYER_COUNT; b++) {
        const email = buyerEmail(b);

        const res = http.post(
            `${IDENTITY_BASE_URL}/api/auth/signup`,
            JSON.stringify({
                email,
                password: BUYER_PASSWORD,
                name: '부하테스트',
                defaultShippingAddress: '서울특별시 강남구 테헤란로 123',
                phoneNumber: '010-1234-5678',
            }),
            {
                headers: JSON_HEADERS,
                tags: { name: 'SEED POST /api/auth/signup (buyer)' },
            }
        );

        if (res.status === 200) {
            buyerMemberIds.push(res.json('data.memberId'));
        } else if (res.status === 409) {
            // 이미 존재하는 계정(재실행). 정상으로 간주한다.
            console.log(`구매 계정 ${email} 이미 존재 (재실행)`);
        } else {
            console.error(`구매 계정 생성 실패 ${email}: ${res.status} ${res.body}`);
        }
    }

    const allIds = immediateIds.concat(offerIds);

    console.log('');
    console.log('=================== 시딩 완료 ===================');
    console.log(
        `상품 ${allIds.length}개 ` +
        `(IMMEDIATE ${immediateIds.length} / OFFER ${offerIds.length})`
    );
    console.log(`구매용 계정 ${buyerMemberIds.length}개 신규 생성`);
    console.log('');
    console.log('[조회 계열 시나리오]');
    console.log(`  -e PRODUCT_IDS=${allIds.join(',')}`);
    console.log('');
    console.log('[offer.js 전용 — saleType=OFFER]');
    console.log(`  -e PRODUCT_IDS=${offerIds.join(',')}`);
    console.log('');
    console.log('[purchase.js write 전용 — saleType=IMMEDIATE]');
    console.log(`  -e PRODUCT_IDS=${immediateIds.join(',')}`);

    if (buyerMemberIds.length > 0) {
        console.log('');
        console.log('[direct 모드로 구매 테스트할 때만 필요]');
        console.log(`  -e BUYER_MEMBER_IDS=${buyerMemberIds.join(',')}`);
    }

    console.log('');
    console.log('다음 단계: 구매(write) 테스트를 하려면 지갑 잔액을 채워야 한다.');
    console.log('  psql -h <DB_HOST> -U <USER> -d dear -f sql/seed-wallet.sql');
    console.log('================================================');
}
