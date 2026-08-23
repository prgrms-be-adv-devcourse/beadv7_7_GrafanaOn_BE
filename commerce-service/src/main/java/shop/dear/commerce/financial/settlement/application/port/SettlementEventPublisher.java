package shop.dear.commerce.financial.settlement.application.port;

import shop.dear.common.event.settlement.SettlementPayoutEvent;

// settlement -> wallet 정산 지급 요청 포트.
// 현재 구현체(SpringSettlementEventPublisher)는 같은 프로세스 안에서 호출자의
// 트랜잭션에 합류해 동기로 처리한다 - 브로커를 경유하는 비동기/최종적 일관성
// 전달을 보장하지 않는다. wallet 이 별도 서비스로 분리되어 실제 비동기 구현체로
// 교체될 경우, 지금처럼 같은 트랜잭션에서의 롤백에 기대지 말고 outbox 패턴과
// 멱등 처리(SettlementPayoutEvent.settlementBatchId 를 멱등키로 사용)를 함께 갖춰야 한다.
public interface SettlementEventPublisher {

	void publish(final SettlementPayoutEvent event);
}
