package shop.dear.commerce.order.purchase.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
public class PurchaseClientConfig {

  @Bean
  RestClient productRestClient(
          @Value("${product.client.base-url}") final String baseUrl
  ) {
    return createRestClient(baseUrl);
  }

  @Bean
  RestClient memberRestClient(
          @Value("${member.client.base-url}") final String baseUrl
  ) {
    return createRestClient(baseUrl);
  }

  private RestClient createRestClient(final String baseUrl) {
    final SimpleClientHttpRequestFactory requestFactory =
            new SimpleClientHttpRequestFactory();

    requestFactory.setConnectTimeout(Duration.ofSeconds(2));
    requestFactory.setReadTimeout(Duration.ofSeconds(3));

    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor(this::relayAuthorizationHeader)
            .build();
  }

  private ClientHttpResponse relayAuthorizationHeader(
          final HttpRequest request,
          final byte[] body,
          final ClientHttpRequestExecution execution
  ) throws IOException {
    final ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes != null) {
      final String authorization =
              attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

      if (authorization != null && !authorization.isBlank()) {
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
      }
    }

    return execution.execute(request, body);
  }
}
