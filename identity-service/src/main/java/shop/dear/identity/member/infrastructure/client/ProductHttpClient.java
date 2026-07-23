package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.port.ProductPort;
import shop.dear.identity.member.infrastructure.client.dto.ProductApiData;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductHttpClient implements ProductPort {

    private final RestClient productRestClient;

    @Override
    public boolean hasProduct() {
        ApiResponse<List<ProductApiData>> body = productRestClient.get()
            .uri("/api/products/me")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        List<ProductApiData> data = body.getData();

        return data != null && !data.isEmpty();
    }
}
