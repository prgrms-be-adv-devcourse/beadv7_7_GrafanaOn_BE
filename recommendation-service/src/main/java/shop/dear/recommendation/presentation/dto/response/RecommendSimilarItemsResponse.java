package shop.dear.recommendation.presentation.dto.response;

import shop.dear.recommendation.domain.model.RecommendationItem;

import java.util.List;
import java.util.stream.IntStream;

public record RecommendSimilarItemsResponse(
	Long productId,
	Integer rank
) {

	public static List<RecommendSimilarItemsResponse> listOf(final List<RecommendationItem> items) {
		return IntStream.range(0, items.size())
			.mapToObj(index -> new RecommendSimilarItemsResponse(items.get(index).productId(), index + 1))
			.toList();
	}
}
