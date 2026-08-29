package shop.dear.commerce.order.purchase.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.ProductPort;

import java.util.List;

// PENDING/FAILED 건을 배치로 조회해 상품 상태 원복 API를 재시도.
// 성공하면 SUCCESS, 실패하면 retryCount 증가 + FAILED 유지하여 다음 폴링 주기에 다시 시도(무한 재시도)
@Slf4j
@Component
public class CompensationOutboxPollingScheduler {

    private final CompensationOutboxRepository compensationOutboxRepository;
    private final ProductPort productPort;
    private final int batchSize;

    public CompensationOutboxPollingScheduler(
        final CompensationOutboxRepository compensationOutboxRepository,
        final ProductPort productPort,
        @Value("${order.compensation-outbox.batch-size:100}") final int batchSize
    ) {
        this.compensationOutboxRepository = compensationOutboxRepository;
        this.productPort = productPort;
        this.batchSize = batchSize;
    }

    @Transactional
    public void retryPendingCompensations() {
        final List<CompensationOutbox> batch = compensationOutboxRepository.findBatchForRetry(
            CompensationOutboxStatus.SUCCESS,
            PageRequest.of(0, batchSize)
        );

        if (batch.isEmpty()) {
            return;
        }

        log.info("[CompensationOutboxPollingScheduler] 상품 상태 보상 재시도 시작. size={}", batch.size());

        for (final CompensationOutbox outbox : batch) {
            try {
                productPort.cancelTradeProduct(outbox.getProductId());
                outbox.markSuccess();
            } catch (final Exception e) {
                outbox.markFailed(e.getMessage());
                log.error("[CompensationOutboxPollingScheduler] 상품 상태 보상 실패. productId={}, retryCount={}",
                        outbox.getProductId(), outbox.getRetryCount(), e);
            }
        }
    }
}
