package shop.dear.identity.member.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberClientConfig {

    @Bean
    RestClient productRestClient(@Value("${identity.client.commerce-base-url}") String baseUrl) {

        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
