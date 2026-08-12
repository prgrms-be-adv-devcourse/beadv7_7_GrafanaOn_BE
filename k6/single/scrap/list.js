import http from 'k6/http';
import { check, sleep } from 'k6';
import { IDENTITY_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    loginAsBuyer,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/scraps
//
// 페이징 조회다. 목록이 비어 있으면 부하 의미가 없으므로 계정별로 몇 건 담아둔다.
//
// ⚠️ 담는 작업은 setup()에서 한다.
//    부하 구간에서 담으면 준비 요청이 측정 구간에 섞이고,
//    담기가 실패한 VU는 조회를 못 해 표본에서 조용히 빠진다.

export const options = Object.assign(
    singleOptions('GET /api/scraps'),
    { setupTimeout: '5m' }
);

const PREP_COUNT = Number(__ENV.PREP_COUNT || 10);

// 최대 VU 수 이상이어야 한다. 모자라면 준비되지 않은 계정이 조회에 참여한다.
const TARGET_COUNT = Number(__ENV.TARGET_COUNT || 50);

// 페이지 번호는 1부터 시작한다.
const PAGE_SIZE = 10;

export function setup() {
    if (!__ENV.PRODUCT_IDS) {
        throw new Error('-e PRODUCT_IDS=... 가 필요합니다. sql/get-product-ids.sql 로 뽑으세요.');
    }

    const productIds = __ENV.PRODUCT_IDS.split(',').map(Number);
    const scrapCount = Math.min(PREP_COUNT, productIds.length);

    for (let index = 1; index <= TARGET_COUNT; index++) {
        const session = loginAsBuyer(index);

        for (let i = 0; i < scrapCount; i++) {
            const res = http.post(
                `${IDENTITY_BASE_URL}/api/scraps/${productIds[i]}`,
                null,
                { headers: authHeaders(session), tags: { name: 'SETUP POST /api/scraps/:productId' } }
            );

            // 이전 실행에서 이미 스크랩했다면 SC-002가 온다.
            // 목록에는 이미 들어 있으므로 준비가 된 것으로 본다.
            const alreadyScrapped = res.body && res.body.includes('SC-002');

            if (res.status !== 200 && !alreadyScrapped) {
                throw new Error(
                    `${index}번 계정의 스크랩 준비 실패. ` +
                    `상품=${productIds[i]} 상태=${res.status} body=${res.body}`
                );
            }
        }
    }

    console.log(`계정 ${TARGET_COUNT}개에 스크랩 ${scrapCount}건씩 준비 완료`);
    return { scrapCount };
}

export default function () {
    const session = getSession();

    const res = http.get(
        `${IDENTITY_BASE_URL}/api/scraps?page=1&size=${PAGE_SIZE}`,
        { headers: authHeaders(session), tags: { name: 'GET /api/scraps' } }
    );

    check(res, { '스크랩 목록 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
