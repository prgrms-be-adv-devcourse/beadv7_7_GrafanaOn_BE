package shop.dear.identity.member.infrastructure.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.dear.common.client.InternalCircuitBreakerConfig;

/**
 * 회원가입 지갑 생성 호출 전용 차단기 설정입니다.
 *
 * 현재 값은 공통 기본값과 같습니다만, 다른 내부 호출이 늘어나 공통 기본값이 조정되더라도
 * 이것은 변경하지 않게 하기 위함입니다.
 */
@Configuration
public class WalletCircuitBreakerConfig {

    public static final String COMMERCE_WALLET = "commerceWallet";

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> walletCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder.circuitBreakerConfig(walletConfig()), COMMERCE_WALLET
        );
    }

    private CircuitBreakerConfig walletConfig() {
        return CircuitBreakerConfig.from(InternalCircuitBreakerConfig.internalCallDefault())
                .build();
    }
}
