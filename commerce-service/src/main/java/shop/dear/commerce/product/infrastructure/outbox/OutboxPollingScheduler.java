package shop.dear.commerce.product.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.infrastructure.client.RecommendationHttpClient;

import java.util.List;

//전달에 성공하면 SENT, 실패하면 FAIL, 재시도 횟수를 올리고 다음 주기에 다시 시도
@Slf4j
@Component
public class OutboxPollingScheduler {

    private final ProductOutboxRepository productOutboxRepository;
    private final RecommendationHttpClient recommendationClient;
    private final int batchSize;

    public OutboxPollingScheduler(
        final ProductOutboxRepository productOutboxRepository,
        final RecommendationHttpClient recommendationClient,
        @Value("${product.outbox.batch-size:100}") final int batchSize
    ) {
        this.productOutboxRepository = productOutboxRepository;
        this.recommendationClient = recommendationClient;
        this.batchSize = batchSize;
    }

    @Transactional
    public void sendPendingEvents() {
        final List<ProductOutbox> batch = productOutboxRepository.findBatchForPublish(
            ProductOutboxStatus.SENT,
            PageRequest.of(0, batchSize)
        );

        if (batch.isEmpty()) {
            return;
        }

        log.info("[OutboxPollingScheduler] recommendation 전달 시작. size={}", batch.size());

        try {
            recommendationClient.sendProductEvents(batch);

            batch.forEach(ProductOutbox::markSent);

            log.info("[OutboxPollingScheduler] recommendation 전달 성공. size={}", batch.size());
        } catch (final Exception e) {
            batch.forEach(outbox -> outbox.markFailed(e.getMessage()));

            log.error("[OutboxPollingScheduler] recommendation 전달 실패. size={}", batch.size(), e);
        }
    }
}
