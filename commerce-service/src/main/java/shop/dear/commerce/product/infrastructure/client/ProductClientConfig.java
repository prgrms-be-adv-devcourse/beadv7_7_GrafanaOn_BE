package shop.dear.commerce.product.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import shop.dear.common.exception.BusinessException;

@Slf4j
@Configuration
public class ProductClientConfig {

    @Bean
    RestClient productRestClient(@Value("${order.client.base-url}") final String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                log.error("[RestClient Error] URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                throw new BusinessException(ProductHttpErrorCode.EXTERNAL_API_ERROR);
            })
            .build();
    }
}
