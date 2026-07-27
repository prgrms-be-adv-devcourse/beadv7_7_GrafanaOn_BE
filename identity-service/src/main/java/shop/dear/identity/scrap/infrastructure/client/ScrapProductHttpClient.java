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
import shop.dear.identity.scrap.infrastructure.client.dto.GetProductsBody;
import shop.dear.identity.scrap.infrastructure.client.dto.ProductApiData;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScrapProductHttpClient implements ProductPort {

    private final RestClient productRestClient;

    @Override
    public List<ProductSummary> getProducts(final List<Long> productIds) {

        if (productIds.isEmpty()) {
            return List.of();
        }

        final ApiResponse<ProductApiData> body = productRestClient.method(HttpMethod.GET)
            .uri("/internal/products/me/scraps")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new GetProductsBody(productIds))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        return body.getData().products().stream()
            .map(product -> new ProductSummary(
                product.id(),
                product.name(),
                product.brand(),
                product.price(),
                product.thumbnailUrl(),
                product.status()
            ))
            .toList();
    }
}
