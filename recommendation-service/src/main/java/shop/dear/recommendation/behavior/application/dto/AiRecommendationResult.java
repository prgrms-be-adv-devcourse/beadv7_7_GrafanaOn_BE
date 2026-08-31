package shop.dear.recommendation.behavior.application.dto;

import java.util.List;

public record AiRecommendationResult(
        List<AiRecommendationItem> items
) {

    public boolean isEmpty() {
        return items == null
                || items.isEmpty();
    }

    public record AiRecommendationItem(
            Long productId,
            Double score
    ) {
    }
}