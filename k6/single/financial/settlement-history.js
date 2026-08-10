import http from 'k6/http';
import { check, sleep } from 'k6';
import { COMMERCE_BASE_URL, THINK_TIME } from '../../config/environment.js';
import { singleOptions } from '../_common.js';
import {
    getSession,
    authHeaders,
} from '../../config/auth.js';

// 측정 대상: GET /api/settlements/me/history?startDate&endDate
//
// 기간 조회다. 정산 이력이 쌓일수록 무거워지는 구간이라 인덱스 유무를 보는 것이 목적이다.
// 조회 기간은 MONTHS 환경변수로 조절한다(기본 1개월).

export const options = singleOptions('GET /api/settlements/me/history');

const MONTHS = Number(__ENV.MONTHS || 1);

function dateRange() {
    const end = new Date();
    const start = new Date();
    start.setMonth(start.getMonth() - MONTHS);

    const fmt = (d) => d.toISOString().slice(0, 10);
    return { startDate: fmt(start), endDate: fmt(end) };
}

export default function () {
    const session = getSession();
    const range = dateRange();

    const res = http.get(
        `${COMMERCE_BASE_URL}/api/settlements/me/history` +
        `?startDate=${range.startDate}&endDate=${range.endDate}`,
        { headers: authHeaders(session), tags: { name: 'GET /api/settlements/me/history' } }
    );

    check(res, { '정산 이력 200': (r) => r.status === 200 });
    sleep(THINK_TIME);
}
