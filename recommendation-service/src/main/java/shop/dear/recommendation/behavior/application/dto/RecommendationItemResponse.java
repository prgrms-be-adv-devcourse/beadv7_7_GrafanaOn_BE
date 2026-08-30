package shop.dear.recommendation.behavior.application.dto;

public record RecommendationItemResponse(
        Long productId,
        Double score,
        Integer rank
) {
}
