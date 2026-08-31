package shop.dear.commerce.financial.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;

@Configuration
@RequiredArgsConstructor
public class OrderPaymentResultClientConfig {

    private final InternalRestClientFactory internalRestClientFactory;

    /**
     * Order 서비스 내부 호출에 사용하는 RestClient를 생성한다.
     *
     * <p>공통 InternalRestClientFactory를 사용해 내부 서비스 호출의 타임아웃과
     * 요청 인터셉터 정책을 일관되게 적용한다.</p>
     */
    @Bean
    RestClient orderPaymentResultRestClient(
            @Value("${order.client.base-url}")
            final String baseUrl
    ) {
        return internalRestClientFactory.builder(baseUrl).build();
    }
}
