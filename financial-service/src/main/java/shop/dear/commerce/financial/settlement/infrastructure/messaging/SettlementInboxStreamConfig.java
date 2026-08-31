package shop.dear.commerce.financial.settlement.infrastructure.messaging;

import com.rabbitmq.stream.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import shop.dear.common.messaging.consumer.RabbitStreamListenerContainerFactory;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SettlementInboxStreamConfig {

    private static final String CONSUMER_NAME = "settlement-order-finished";

    private final Environment environment;
    private final RabbitStreamListenerContainerFactory containerFactory;

    @Bean
    public StreamListenerContainer orderFinishedStreamListenerContainer(
            final OrderFinishedStreamMessageHandler messageHandler
    ) {
        declareStreamIfAbsent();

        return containerFactory.create(
                OrderFinishedStreamMessageHandler.ORDER_FINISHED_STREAM,
                CONSUMER_NAME,
                messageHandler
        );
    }

    private void declareStreamIfAbsent() {
        final String streamName = OrderFinishedStreamMessageHandler.ORDER_FINISHED_STREAM;

        if (environment.streamExists(streamName)) {
            return;
        }

        environment.streamCreator().stream(streamName).create();
        log.info("RabbitMQ Stream 을 생성했습니다: {}", streamName);
    }
}
