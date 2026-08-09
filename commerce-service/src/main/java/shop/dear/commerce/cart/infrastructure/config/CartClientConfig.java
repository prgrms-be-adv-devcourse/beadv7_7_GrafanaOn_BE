package shop.dear.commerce.cart.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.product.infrastructure.client.ProductHttpErrorCode;
import shop.dear.common.client.InternalRestClientFactory;
import shop.dear.common.exception.BusinessException;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class CartClientConfig {

    private final InternalRestClientFactory internalRestClientFactory;

    @Bean
    RestClient cartProductRestClient(@Value("${product.client.base-url}") final String baseUrl) {
        return internalRestClientFactory.builder(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    log.error("[CartProductRestClient Error] URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                    throw new BusinessException(ProductHttpErrorCode.EXTERNAL_API_ERROR);
                })
                .build();
    }

}
