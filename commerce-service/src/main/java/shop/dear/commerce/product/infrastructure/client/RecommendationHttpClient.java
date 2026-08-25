package shop.dear.commerce.product.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.product.infrastructure.outbox.ProductOutbox;
import shop.dear.commerce.product.infrastructure.client.dto.ProductEventApiRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
public class RecommendationHttpClient {

    private final RestClient recommendationRestClient;
    private final ObjectMapper objectMapper;

    public RecommendationHttpClient(
        @Qualifier("productRecommendationRestClient") final RestClient recommendationRestClient,
        final ObjectMapper objectMapper
    ) {
        this.recommendationRestClient = recommendationRestClient;
        this.objectMapper = objectMapper;
    }

    public void sendProductEvents(final List<ProductOutbox> batch) {
        log.info("[RecommendationHttpClient] recommendation - 상품 이벤트 전달 요청. size={}", batch.size());

        recommendationRestClient.post()
            .uri("/internal/recommendation/product-events")
            .body(toApiRequests(batch))
            .retrieve()
            .toBodilessEntity();

        log.info("[RecommendationHttpClient] recommendation - 상품 이벤트 전달 요청 성공. size={}", batch.size());
    }

    /**
     * payload는 jsonb에 저장된 JSON 문자열이라 그대로 담으면 문자열로 한 번 더 감싸집니다.
     * 소비 측이 바로 객체로 읽을 수 있도록 JsonNode로 풀어서 보냅니다.
     */
    private List<ProductEventApiRequest> toApiRequests(final List<ProductOutbox> batch) {
        return batch.stream()
            .map(outbox -> new ProductEventApiRequest(
                outbox.getId(),
                outbox.getAggregateType(),
                outbox.getAggregateId(),
                outbox.getEventType().name(),
                objectMapper.readTree(outbox.getPayload()),
                outbox.getInsertedAt()
            ))
            .toList();
    }
}
