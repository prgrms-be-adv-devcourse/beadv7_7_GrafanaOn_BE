package shop.dear.commerce.search.infrastructure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SearchResilienceConfig {

    public static final String ELASTICSEARCH = "elasticsearch";

    // JPA 검색은 p95 4.5초에 실패율이 87%였다.
    // 초기값 10으로 시작하고 재측정 결과에 따라 조정할 계획이다.
    private static final int MAX_CONCURRENT_JPA_SEARCHES = 10;

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> searchCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder.circuitBreakerConfig(elasticsearchConfig()), ELASTICSEARCH
        );
    }

    private CircuitBreakerConfig elasticsearchConfig() {
        return CircuitBreakerConfig.custom()
                // 검색은 사용자 트래픽이니 시간으로 판단한다.
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(30) // 최근 30초 안의 호출만 보고 판단한다.
                .minimumNumberOfCalls(20) // 30초 안에 20건의 요청이 모여야 실패율을 계산할 수 있다.

                .failureRateThreshold(50) // 실패율이 50%를 넘으면 연다.

                // 실측 p95 90ms였으니 1초를 넘으면 정상이 아니다.
                .slowCallRateThreshold(80) // 느린 호출이 80%를 넘어도 연다.
                .slowCallDurationThreshold(Duration.ofSeconds(1)) // 1초를 넘긴 호출을 느린 호출로 명시한다.

                .waitDurationInOpenState(Duration.ofSeconds(10)) // 10초 간은 ES를 아예 안 부르고 JPA로만 검색을 실시한다.
                .permittedNumberOfCallsInHalfOpenState(5) // 그 후 시험 호출 5건을 보내 회복을 확인한다. 5건이 실패하면 다시 닫고 10초를 더 기다린다.
                .automaticTransitionFromOpenToHalfOpenEnabled(true) // 자동 전환, 10초가 지나고 그 후 요청이 오지 않아도 알아서 HALF_OPEN 상태로 변환.
                .build();
    }

    @Bean
    public Bulkhead jpaSearchFallbackBulkhead() {
        return Bulkhead.of("jpaSearchFallback", BulkheadConfig.custom()
                .maxConcurrentCalls(MAX_CONCURRENT_JPA_SEARCHES)
                // 대기하지 않는다.
                .maxWaitDuration(Duration.ZERO)
                .build()
        );
    }
}
