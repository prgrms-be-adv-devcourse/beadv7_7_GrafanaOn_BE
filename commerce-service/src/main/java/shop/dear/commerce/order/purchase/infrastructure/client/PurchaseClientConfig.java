package shop.dear.commerce.order.purchase.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;

@Configuration
@RequiredArgsConstructor
public class PurchaseClientConfig {
  private final InternalRestClientFactory internalRestClientFactory;

  @Bean
  RestClient productRestClient(
          @Value("${product.client.base-url}") final String baseUrl
  ) {
    return internalRestClientFactory.builder(baseUrl).build();
  }

  @Bean
  RestClient memberRestClient(
          @Value("${member.client.base-url}") final String baseUrl
  ) {
    return internalRestClientFactory.builder(baseUrl).build();
  }
}
