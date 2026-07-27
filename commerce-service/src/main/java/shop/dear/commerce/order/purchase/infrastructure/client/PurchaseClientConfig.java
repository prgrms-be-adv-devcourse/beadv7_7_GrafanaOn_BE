package shop.dear.commerce.order.purchase.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PurchaseClientConfig {

  @Bean
  RestClient purchaseRestClient(@Value("${order.client.base-url}") String baseUrl) {
    return createRestClient(baseUrl);
  }

  @Bean
  RestClient memberRestClient(@Value("${member.client.base-url:${order.client.base-url}}") String baseUrl) {
    return createRestClient(baseUrl);
  }

  private RestClient createRestClient(final String baseUrl) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(2));
    requestFactory.setReadTimeout(Duration.ofSeconds(3));

    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
  }
}
