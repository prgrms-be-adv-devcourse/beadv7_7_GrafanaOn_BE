package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;

@Configuration
@RequiredArgsConstructor
public class MemberClientConfig {
    private final InternalRestClientFactory internalRestClientFactory;

    @Bean
    RestClient productRestClient(@Value("${identity.client.commerce-base-url}") String baseUrl) {

        return internalRestClientFactory.builder(baseUrl).build();
    }

    @Bean
    RestClient walletRestClient(@Value("${identity.client.commerce-base-url}") String baseUrl) {

        return internalRestClientFactory.builder(baseUrl).build();
    }

    @Bean
    RestClient authRestClient(@Value("${identity.client.auth-base-url}") String baseUrl) {

        return internalRestClientFactory.builder(baseUrl).build();
    }
}
