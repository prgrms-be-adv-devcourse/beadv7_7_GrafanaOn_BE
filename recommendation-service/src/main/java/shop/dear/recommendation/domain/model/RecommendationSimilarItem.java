package shop.dear.recommendation.domain.model;

//조회 결과
public record RecommendationSimilarItem(
	Long productId,
	double distance
) {
}
