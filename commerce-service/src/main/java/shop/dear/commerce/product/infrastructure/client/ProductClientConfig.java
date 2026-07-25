package shop.dear.commerce.product.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import shop.dear.common.exception.BusinessException;

import java.io.IOException;

@Slf4j
@Configuration
public class ProductClientConfig {

    @Bean
    RestClient productRestClient(@Value("${order.client.base-url}") final String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(this::relayAuthorizationHeader)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                log.error("[RestClient Error] URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                throw new BusinessException(ProductHttpErrorCode.EXTERNAL_API_ERROR);
            })
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
            final String authHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && !authHeader.isBlank()) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
            }
        }

        return execution.execute(request, body);
    }
}
