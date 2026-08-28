package shop.dear.commerce.financial.payment.infrastructure.client.toss;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TossPaymentClientConfig {
    @Bean("tossPaymentRestClient")
    RestClient tossPaymentRestClient(
            @Value("${toss.base-url:https://api.tosspayments.com}")
            final String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
