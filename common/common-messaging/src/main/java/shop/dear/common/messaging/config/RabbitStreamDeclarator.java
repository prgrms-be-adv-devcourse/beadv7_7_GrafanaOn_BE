package shop.dear.common.messaging.config;

import com.rabbitmq.stream.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitStreamDeclarator {

    private final Environment environment;

    public void declareStream(final String streamName) {
        if (environment.streamExists(streamName)) {
            log.info("RabbitMQ Stream이 이미 존재합니다: {}", streamName);
            return;
        }

        environment.streamCreator().stream(streamName).create();
        log.info("RabbitMQ Stream을 생성했습니다: {}", streamName);
    }
}
