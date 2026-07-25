package shop.dear.commerce.product.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.product.application.dto.external.ExistsOffer;
import shop.dear.commerce.product.application.port.OfferPort;
import shop.dear.commerce.product.infrastructure.client.dto.ExistsOfferApiData;
import shop.dear.common.response.ApiResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class OfferHttpClient implements OfferPort {

    private final RestClient productRestClient;

    @Override
    public ExistsOffer existsOffer(final Long productId) {
        log.info("[OfferHttpClient] offer - 오퍼 존재 여부 조회 요청. productId={}", productId);

        final ApiResponse<ExistsOfferApiData> body = productRestClient.get()
            .uri("/offers/{productId}/status")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        final ExistsOfferApiData data = body.getData();

        log.info("[OfferHttpClient] offer - 오퍼 존재 여부 조회 요청 성공. productId={} exists={}", productId, data.exists());

        return new ExistsOffer(data.exists());
    }
}
