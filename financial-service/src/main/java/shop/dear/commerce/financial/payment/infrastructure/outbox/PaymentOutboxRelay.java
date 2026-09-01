package shop.dear.commerce.financial.payment.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 아직 전송되지 않은 Payment Outbox를 Order 서비스로 전달하는 스케줄러
 *
 * <p>여러 Financial 인스턴스가 실행돼도 같은 Outbox를 동시에 조회·전송하지 않도록
 * ShedLock을 적용한다.
 * 실제 한 건의 전송과 상태 변경은 별도 트랜잭션인
 * PaymentOutboxRelayProcessor가 담당한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PaymentOutboxRelay {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentOutboxRelayProcessor paymentOutboxRelayProcessor;

    @Value("${payment.outbox.relay.max-retry-count:3}")
    private int maxRetryCount;

    @Scheduled(fixedDelayString = "${payment.outbox.relay.fixed-delay:5000}")
    @SchedulerLock(
            name = "payment_outbox_relay",
            // 최대 100건을 순차 전송할 때 HTTP 타임아웃을 고려해 충분한 락 시간을 둔다.
            lockAtMostFor = "10m",
            lockAtLeastFor = "0s"
    )
    public void relay() {
        final List<PaymentOutbox> outboxes =
                paymentOutboxRepository.findTop100ByStatusInOrderByInsertedAtAsc(
                        List.of(
                                PaymentOutboxStatus.PENDING,
                                PaymentOutboxStatus.FAILED
                        )
                );

        outboxes.forEach(outbox ->
                paymentOutboxRelayProcessor.process(
                        outbox.getId(),
                        maxRetryCount
                )
        );
    }
}
