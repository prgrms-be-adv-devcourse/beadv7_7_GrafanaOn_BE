package shop.dear.commerce.order.purchase.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationOutboxScheduler {

    private final CompensationOutboxPollingScheduler compensationOutboxPollingScheduler;

    @Scheduled(fixedDelay = 5000)
    public void runCompensationRetryJob() {
        compensationOutboxPollingScheduler.retryPendingCompensations();
    }
}
