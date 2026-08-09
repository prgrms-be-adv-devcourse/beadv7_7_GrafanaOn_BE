package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.dto.external.ExistsProduct;
import shop.dear.identity.member.application.port.ProductPort;
import shop.dear.identity.member.infrastructure.client.dto.ProductApiData;

@Component
@RequiredArgsConstructor
public class ProductHttpClient implements ProductPort {

    private final RestClient productRestClient;

    @Override
    public ExistsProduct existsProduct() {
        ApiResponse<ProductApiData> body = productRestClient.get()
            .uri("/internal/products/me/exists")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        ProductApiData data = body.getData();

        return new ExistsProduct(data.exists());
    }
}
