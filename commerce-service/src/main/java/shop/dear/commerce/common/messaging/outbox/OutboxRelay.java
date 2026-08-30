package shop.dear.commerce.common.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import shop.dear.common.messaging.publisher.StreamPublisher;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final String ORDER_FINISHED_STREAM = "order.finished";
    private static final String ORDER_FINISHED_DLQ_STREAM = "order.finished.dlq";
    private static final int MAX_RETRY = 3;
    private static final int PUBLISH_TIMEOUT_SECONDS = 5;

    private final OutboxMessageRepository outboxMessageRepository;
    private final StreamPublisher streamPublisher;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:5000}")
    public void pollAndPublish() {
        final List<OutboxMessage> pendingMessages = outboxMessageRepository
                .findTop100ByStatusOrderByInsertedAtAsc(OutboxMessageStatus.PENDING);

        for (final OutboxMessage message : pendingMessages) {
            transactionTemplate.executeWithoutResult(status -> process(message));
        }
    }

    private void process(final OutboxMessage message) {
        final OutboxMessage freshMessage = outboxMessageRepository.findById(message.getId())
                .orElse(null);

        if (freshMessage == null || freshMessage.getStatus() != OutboxMessageStatus.PENDING) {
            return;
        }

        try {
            streamPublisher.publish(
                    freshMessage.getStreamName(),
                    freshMessage.getEventId(),
                    freshMessage.getEventType(),
                    freshMessage.getPayload()
            ).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            freshMessage.markPublished();
            log.debug("Outbox 메시지 발행 완료: eventId={}", freshMessage.getEventId());
        } catch (final Exception e) {
            log.error("Outbox 메시지 발행 실패: eventId={}, retryCount={}",
                    freshMessage.getEventId(), freshMessage.getRetryCount(), e);
            freshMessage.fail(e.getMessage());

            if (freshMessage.getRetryCount() >= MAX_RETRY) {
                moveToDlq(freshMessage);
            }
        }
    }

    private void moveToDlq(final OutboxMessage message) {
        message.markDlq();
        log.error("Outbox 메시지를 DLQ로 이동합니다: eventId={}", message.getEventId());

        try {
            streamPublisher.publish(
                    ORDER_FINISHED_DLQ_STREAM,
                    message.getEventId(),
                    message.getEventType(),
                    message.getPayload()
            ).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final Exception e) {
            log.error("DLQ 발행 실패: eventId={}", message.getEventId(), e);
        }
    }
}
