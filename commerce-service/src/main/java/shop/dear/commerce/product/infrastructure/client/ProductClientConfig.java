package shop.dear.commerce.product.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;
import shop.dear.common.exception.BusinessException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductClientConfig {

    private final InternalRestClientFactory internalRestClientFactory;

    @Bean
    RestClient productMemberRestClient(@Value("${member.client.base-url}") final String baseUrl) {
        return internalRestClientFactory.builder(baseUrl)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                log.error("[MemberRestClient Error] URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                throw new BusinessException(ProductHttpErrorCode.EXTERNAL_API_ERROR);
            })
            .build();
    }

    @Bean
    RestClient productOfferRestClient(@Value("${offer.client.base-url}") final String baseUrl) {
        return internalRestClientFactory.builder(baseUrl)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                log.error("[OfferRestClient Error] URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                throw new BusinessException(ProductHttpErrorCode.EXTERNAL_API_ERROR);
            })
            .build();
    }
}
