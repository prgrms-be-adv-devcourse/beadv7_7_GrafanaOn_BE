package shop.dear.commerce.financial.payment.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.payment.infrastructure.client.OrderPaymentResultClient;

/**
 * Payment Outbox 한 건을 독립 트랜잭션에서 전송하고 상태를 변경한다.
 *
 * <p>한 메시지의 전송 실패가 다른 Outbox 처리까지 롤백시키지 않도록
 * REQUIRES_NEW 트랜잭션을 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxRelayProcessor {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final OrderPaymentResultClient orderPaymentResultClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(final Long outboxId, final int maxRetryCount) {
        final PaymentOutbox outbox = paymentOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment Outbox를 찾을 수 없습니다. outboxId=" + outboxId
                ));

        try {
            orderPaymentResultClient.send(outbox);
            // HTTP 응답 성공 후에만 SENT로 변경한다.
            outbox.markSent();
        } catch (final RuntimeException exception) {
            // HTTP 호출 또는 JSON 변환 실패를 기록하고 다음 Relay 주기에 재시도한다.
            outbox.markFailed(exception.getMessage());

            if (outbox.getRetryCount() >= maxRetryCount) {
                outbox.markExhausted();

                log.error(
                        "Payment Outbox 재시도 횟수를 초과했습니다. eventId={}, retryCount={}",
                        outbox.getEventId(),
                        outbox.getRetryCount()
                );
            }
        }
    }
}
