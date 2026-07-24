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
    // @AuthUser의 AuthUserArgumentResolver가 읽는 것과 같다.
    private static final String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";

    @Override
    public ExistsProduct existsProduct(final Long memberId) {
        ApiResponse<ProductApiData> body = productRestClient.get()
                .uri("/api/products/me/exists")
                .header(MEMBER_ID_HEADER, memberId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        ProductApiData data = body.getData();

        return new ExistsProduct(data.exists());
    }
}
