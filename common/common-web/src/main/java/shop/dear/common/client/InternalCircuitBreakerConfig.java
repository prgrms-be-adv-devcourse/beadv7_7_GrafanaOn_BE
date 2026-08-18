package shop.dear.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class InternalCircuitBreakerConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> internalCallCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
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
                        .build())
                .build());
    }
}
