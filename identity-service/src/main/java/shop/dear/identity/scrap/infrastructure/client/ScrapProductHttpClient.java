package shop.dear.identity.scrap.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.scrap.application.dto.external.ProductSummary;
import shop.dear.identity.scrap.application.port.ProductPort;
import shop.dear.identity.scrap.infrastructure.client.dto.ProductIdsRequest;
import shop.dear.identity.scrap.infrastructure.client.dto.ProductSummariesApiData;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScrapProductHttpClient implements ProductPort {

    private final RestClient productRestClient;

    @Override
    public List<ProductSummary> findProducts(final List<Long> productIds) {

        if (productIds.isEmpty()) {
            return List.of();
        }

        final ApiResponse<ProductSummariesApiData> body = productRestClient.method(HttpMethod.GET)
            .uri("/internal/products/scrap/me")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ProductIdsRequest(productIds))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        return body.getData().products().stream()
            .map(product -> new ProductSummary(
                product.productId(),
                product.name(),
                product.brand(),
                product.price(),
                product.thumbnailUrl(),
                product.status()
            ))
            .toList();
    }
}
