package shop.dear.commerce.common.messaging.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import shop.dear.commerce.common.event.FinishedOrderEventPublisher;
import shop.dear.common.event.order.FinishedOrderEvent;

@Component
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "false")
public class NoOpFinishedOrderEventPublisher implements FinishedOrderEventPublisher {

    @Override
    public void publish(final FinishedOrderEvent event) {
        // Stream 메시징이 비활성화된 경우 아웃박스에 기록하지 않는다.
        // 운영 환경에서는 RabbitMQ Stream이 활성화되어야 하며, 테스트 환경에서만 사용된다.
    }
}
