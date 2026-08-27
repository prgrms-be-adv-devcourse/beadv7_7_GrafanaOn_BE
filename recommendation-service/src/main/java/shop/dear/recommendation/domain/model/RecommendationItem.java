package shop.dear.recommendation.domain.model;

//조회 결과
public record RecommendationItem(
	Long productId,
	double distance
) {
}
