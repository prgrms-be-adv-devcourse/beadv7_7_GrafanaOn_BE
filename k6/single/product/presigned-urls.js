import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authJsonHeaders,
} from '../../config/auth.js';

// 측정 대상: POST /api/products/images/presigned-urls
//
// S3 Presigned URL 생성은 로컬 서명 연산이라 AWS로 네트워크 호출이 나가지 않는다.
// 서명 비용과 판매자 조회 비용을 측정한다.

export const options = singleOptions('POST /api/products/images/presigned-urls');

export default function () {
    const session = getSession();

    const res = http.post(
        `${COMMERCE_BASE_URL}/api/products/images/presigned-urls`,
        JSON.stringify({
            files: [
                { sortOrder: 1, uploadFileType: 'PNG' },
                { sortOrder: 2, uploadFileType: 'PNG' },
                { sortOrder: 3, uploadFileType: 'PNG' },
            ],
        }),
        {
            headers: authJsonHeaders(session),
            tags: { name: 'POST /api/products/images/presigned-urls' },
        }
    );

    check(res, { 'Presigned URL 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
