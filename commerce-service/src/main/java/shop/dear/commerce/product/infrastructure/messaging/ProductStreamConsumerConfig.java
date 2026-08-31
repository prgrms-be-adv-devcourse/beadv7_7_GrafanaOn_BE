package shop.dear.commerce.product.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import shop.dear.common.messaging.config.RabbitStreamDeclarator;
import shop.dear.common.messaging.consumer.RabbitStreamListenerContainerFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductStreamConsumerConfig {

    private final RabbitStreamDeclarator rabbitStreamDeclarator;
    private final RabbitStreamListenerContainerFactory containerFactory;
    private final FinishedOrderEventMessageHandler messageHandler;

    @Bean
    public StreamListenerContainer orderFinishedStreamListenerContainer() {
        rabbitStreamDeclarator.declareStream(FinishedOrderStream.NAME);

        return containerFactory.create(
                FinishedOrderStream.NAME,
                FinishedOrderStream.CONSUMER_NAME,
                messageHandler
        );
    }
}
