package shop.dear.common.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "messaging.rabbitmq.stream")
public record RabbitStreamProperties(
        String host,

        @DefaultValue("5552")
        int port,

        String username,
        String password,

        @DefaultValue("/")
        String virtualHost,

        // 처리 확정 실패 후 Consumer Container를 다시 시작하기 전 대기 시간
        @DefaultValue("5s")
        Duration consumerRestartDelay
) {
        // 로그 출력 시 RabbitMQ 접속 credential이 노출되지 않도록 마스킹한다.
        @Override
        public String toString() {
                return ("RabbitStreamProperties[host=%s, port=%d, username=****, password=****, "
                        + "virtualHost=%s, consumerRestartDelay=%s]")
                        .formatted(
                                host,
                                port,
                                virtualHost,
                                consumerRestartDelay
                        );
        }
}
