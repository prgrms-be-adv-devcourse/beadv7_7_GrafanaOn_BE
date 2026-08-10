import { stages } from '../config/stages.js';

/**
 * 단일 엔드포인트 측정용 옵션 생성기.
 *
 * 시나리오 테스트(k6/scenarios/)는 여러 API를 한 흐름으로 묶어 "실사용 패턴"을 재현한다.
 * 그 방식은 VU 50이어도 개별 API가 받는 동시 요청은 2~3건에 그친다.
 * 이 폴더의 스크립트는 반대로 모든 VU가 하나의 엔드포인트에 집중해 그 API의 한계를 잰다.
 *
 * @param target 측정 대상 요청의 tags.name.
 *               선행 준비 요청(PREP 태그)이 섞여도 이 태그의 p95만 따로 집계된다.
 * @param p95Ms  목표 응답시간(ms). 기본 100.
 */
export function singleOptions(target, p95Ms) {
    const limit = p95Ms === undefined ? 100 : p95Ms;

    return {
        stages,
        thresholds: {
            http_req_failed: ['rate<0.01'],
            [`http_req_duration{name:${target}}`]: [`p(95)<${limit}`],
        },
    };
}
