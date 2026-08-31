package shop.dear.commerce.order.infrastructure.client.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.dear.common.client.InternalCircuitBreakerConfig;

@Configuration
public class FinancialCircuitBreakerConfig {

    public static final String FINANCIAL_PAYMENT = "financialPayment";
    public static final String FINANCIAL_WALLET_HOLD = "financialWalletHold";
    public static final String FINANCIAL_WALLET_RELEASE = "financialWalletRelease";

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> financialPaymentCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder.circuitBreakerConfig(financialConfig()),
                FINANCIAL_PAYMENT
        );
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> financialWalletHoldCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder.circuitBreakerConfig(financialConfig()),
                FINANCIAL_WALLET_HOLD
        );
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> financialWalletReleaseCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder.circuitBreakerConfig(financialConfig()),
                FINANCIAL_WALLET_RELEASE
        );
    }

    private CircuitBreakerConfig financialConfig() {
        return CircuitBreakerConfig.from(InternalCircuitBreakerConfig.internalCallDefault())
                .build();
    }
}
