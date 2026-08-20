package shop.dear.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

@Configuration
public class InternalCircuitBreakerConfig {

    /**
     * Spring Cloud의 기본 1초 TimeLimiter 존재
     * InternalRestClientFactory의 읽기 타임아웃인 3초보다 짧다.
     * 의도적으로 명시한다.
     */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> internalCallCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(internalCallDefault())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(CALL_TIMEOUT)
                        .build())
                .build());
    }

    // 이름별 설정에서 이 값을 기반으로 확장한다.
    public static CircuitBreakerConfig internalCallDefault() {
        return CircuitBreakerConfig.custom()
                // 부하 테스트 기준 초당 10 ~ 30건이 오가므로 최근 20건으로 판단한다.
                .slidingWindowSize(20)

                // 표본이 적을 때 성급하게 판단하지 않고 지켜본다.
                .minimumNumberOfCalls(10)

                // 절반 이상이 실패하면 발동시킨다.
                .failureRateThreshold(50)

                // 10초 뒤 시험 호출 3건으로 회복을 확인한다.
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)

                // 틀린 요청에 대해서는 예외를 세지 않는다.
                .recordException(InternalCircuitBreakerConfig::isSystemFailure)
                .build();
    }

    // 협력 서비스의 장애로 볼 예외만 실패로 보기 위해 isSystemFailure 블랙리스트를 짠다.
    private static boolean isSystemFailure(final Throwable throwable) {
        if (throwable instanceof HttpClientErrorException exception) {
            // 4xx는 요청이 잘못된 것으로 본다. 다만 429는 과부하라는 신호이므로 실패로 센다.
            return exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value();
        }

        return true;
    }
}
