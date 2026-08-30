package shop.dear.recommendation.behavior.application.dto;

import java.util.List;

public record BasicRecommendationResponse(
        String recommendationId,
        List<RecommendationItemResponse> items
) {
    public static BasicRecommendationResponse empty() {
        return new BasicRecommendationResponse(
                null,
                List.of()
        );
    }
}
