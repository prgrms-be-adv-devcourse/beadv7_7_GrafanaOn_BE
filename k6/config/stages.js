import { TEST_TYPE } from './environment.js';

// Load Test: VU 1 → 10 → 20 → 30 → 40 → 50
// 각 단계 10초 램프업 후 1분 30초 유지.
// 유지 구간의 응답시간과 서버 CPU/메모리(Grafana)를 정상상태 기준으로 측정한다.
export const loadTestStages = [
    { duration: '10s', target: 1 },
    { duration: '1m30s', target: 1 },
    { duration: '10s', target: 10 },
    { duration: '1m30s', target: 10 },
    { duration: '10s', target: 20 },
    { duration: '1m30s', target: 20 },
    { duration: '10s', target: 30 },
    { duration: '1m30s', target: 30 },
    { duration: '10s', target: 40 },
    { duration: '1m30s', target: 40 },
    { duration: '10s', target: 50 },
    { duration: '1m30s', target: 50 },
    { duration: '10s', target: 0 },
];

// Stress Test: 목표치(50 VU)를 넘어서 한계점을 찾는다.
// thresholds가 깨지는 시점 = 시스템 한계로 판단한다.
export const stressTestStages = [
    { duration: '10s', target: 50 },
    { duration: '1m', target: 50 },
    { duration: '10s', target: 100 },
    { duration: '1m', target: 100 },
    { duration: '10s', target: 150 },
    { duration: '1m', target: 150 },
    { duration: '10s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '10s', target: 0 },
];

// Burst: 구매(write) 전용 단발성 측정.
// 구매가 성사되면 상품이 판매완료로 바뀌어 재사용이 불가능하므로,
// 상품 소모량을 감당 가능한 수준(VU 10 / 30초 ≈ 90건)으로 제한한다.
// purchase.js가 MODE=write일 때 자동으로 이 단계를 사용한다.
export const burstStages = [
    { duration: '5s', target: 10 },
    { duration: '30s', target: 10 },
    { duration: '5s', target: 0 },
];

// Focus: 단일 엔드포인트 정밀 측정용.
// 개선 전후를 여러 번 비교할 때 10분씩 쓰기엔 부담이 커서 구간을 줄였다.
// VU 10 / 25 / 50 세 지점으로 저하 추세를 보고, 마지막 구간을 1분 유지해 정상상태를 잡는다.
//
// ⚠️ 워밍업이 짧아 JVM JIT 최적화가 덜 된 상태의 수치가 나온다.
//    개발 중 반복 확인용이며, 최종 보고 수치는 load 단계로 다시 측정할 것.
export const focusStages = [
    { duration: '10s', target: 10 },
    { duration: '30s', target: 10 },
    { duration: '10s', target: 25 },
    { duration: '30s', target: 25 },
    { duration: '10s', target: 50 },
    { duration: '1m', target: 50 },
    { duration: '10s', target: 0 },
];

export const stages =
    TEST_TYPE === 'stress' ? stressTestStages
    : TEST_TYPE === 'focus' ? focusStages
    : loadTestStages;
