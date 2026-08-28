package shop.dear.common.messaging.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RabbitMQ Stream 설정")
class RabbitStreamPropertiesTest {

    @Test
    @DisplayName("재시작 최대 횟수가 0이면 예외가 발생한다")
    void constructor_zeroRestartMaxAttempts_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties(Duration.ofSeconds(5), 0)
        );
    }

    @Test
    @DisplayName("재시작 대기 시간이 0이면 예외가 발생한다")
    void constructor_zeroRestartDelay_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties(Duration.ZERO, 5)
        );
    }

    @Test
    @DisplayName("재시작 대기 시간이 음수이면 예외가 발생한다")
    void constructor_negativeRestartDelay_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties(Duration.ofSeconds(-1), 5)
        );
    }

    private RabbitStreamProperties properties(
            final Duration consumerRestartDelay,
            final int consumerRestartMaxAttempts
    ) {
        return new RabbitStreamProperties(
                "localhost",
                5552,
                "guest",
                "guest",
                "/",
                consumerRestartDelay,
                consumerRestartMaxAttempts
        );
    }

    @Test
    @DisplayName("유효한 재시작 설정이면 객체를 생성한다")
    void constructor_validRestartSettings_createsProperties() {
        assertDoesNotThrow(
                () -> properties(Duration.ofSeconds(5), 5)
        );
    }
}