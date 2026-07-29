package shop.dear.commerce.order.offer.infrastructure.client.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;

@Configuration
@RequiredArgsConstructor
public class OfferClientConfig {
    private final InternalRestClientFactory internalRestClientFactory;

    @Bean
    RestClient offerProductRestClient(
            @Value("${order.client.base-url}") final String baseUrl
    ) {
        return internalRestClientFactory.builder(baseUrl).build();
    }

    @Bean
    RestClient offerMemberRestClient(
            @Value("${member.client.base-url:${order.client.base-url}}") final String baseUrl
    ) {
        return internalRestClientFactory.builder(baseUrl).build();
    }
}
