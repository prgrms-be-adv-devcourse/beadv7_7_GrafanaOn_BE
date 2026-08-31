package shop.dear.commerce.common.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.common.event.FinishedOrderEventPublisher;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.messaging.serializer.JsonPayloadSerializer;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxFinishedOrderEventPublisher implements FinishedOrderEventPublisher {

    private static final String EVENT_TYPE = "FinishedOrderEvent";
    private static final String STREAM_NAME = "order.finished";

    private final OutboxMessageRepository outboxMessageRepository;
    private final JsonPayloadSerializer jsonPayloadSerializer;

    @Transactional
    @Override
    public void publish(final FinishedOrderEvent event) {
        final OutboxMessage message = OutboxMessage.create(
                EVENT_TYPE,
                event.orderType(),
                event.orderId(),
                STREAM_NAME,
                jsonPayloadSerializer.serialize(event)
        );
        outboxMessageRepository.save(message);
    }
}
