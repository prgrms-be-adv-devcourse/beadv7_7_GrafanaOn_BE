package shop.dear.commerce.common.messaging.outbox;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import shop.dear.common.messaging.config.RabbitStreamDeclarator;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderFinishedStreamInitializer {

    private static final String ORDER_FINISHED_STREAM = "order.finished";
    private static final String ORDER_FINISHED_DLQ_STREAM = "order.finished.dlq";

    private final RabbitStreamDeclarator rabbitStreamDeclarator;

    @PostConstruct
    public void initialize() {
        rabbitStreamDeclarator.declareStream(ORDER_FINISHED_STREAM);
        rabbitStreamDeclarator.declareStream(ORDER_FINISHED_DLQ_STREAM);
    }
}
