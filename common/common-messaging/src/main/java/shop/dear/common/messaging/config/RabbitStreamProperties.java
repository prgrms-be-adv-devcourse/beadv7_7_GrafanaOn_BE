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
        Duration consumerRestartDelay,

        // Container 재시작이 연속으로 실패했을 때 허용할 최대 시도 횟수
        @DefaultValue("5")
        int consumerRestartMaxAttempts
) {

        public RabbitStreamProperties {
                // 재시작 최대 횟수가 0 이하이면 잘못된 설정이므로,
                // 애플리케이션 시작 시 설정 바인딩을 실패시킨다.
                if (consumerRestartMaxAttempts < 1) {
                        throw new IllegalArgumentException(
                                "consumerRestartMaxAttempts는 1 이상이어야 합니다."
                        );
                }

                // 재시작 대기 시간이 0 이하이면 재시도가 즉시 반복될 수 있으므로,
                // 애플리케이션 시작 시 설정 바인딩을 실패시킨다.
                if (consumerRestartDelay.isZero() || consumerRestartDelay.isNegative()) {
                        throw new IllegalArgumentException(
                                "consumerRestartDelay는 0보다 커야 합니다."
                        );
                }
        }

        // 로그 출력 시 RabbitMQ 접속 credential이 노출되지 않도록 마스킹한다.
        @Override
        public String toString() {
                return ("RabbitStreamProperties[host=%s, port=%d, username=****, password=****, "
                        + "virtualHost=%s, consumerRestartDelay=%s, consumerRestartMaxAttempts=%d]")
                        .formatted(
                                host,
                                port,
                                virtualHost,
                                consumerRestartDelay,
                                consumerRestartMaxAttempts
                        );
        }
}
