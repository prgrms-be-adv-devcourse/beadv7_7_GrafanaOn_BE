package shop.dear.recommendation.behavior.application.dto;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

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

    public static BasicRecommendationResponse toResponse(
            final AiRecommendationResult aiResult,
            final String recommendationId,
            final int limit
    ) {
        List<AiRecommendationResult.AiRecommendationItem> sorted =
                aiResult.items().stream()
                        .sorted(
                                Comparator.comparingDouble(
                                        AiRecommendationResult.AiRecommendationItem::score
                                ).reversed()
                        )
                        .limit(limit)
                        .toList();

        List<RecommendationItemResponse> items =
                IntStream.range(
                                0,
                                sorted.size()
                        )
                        .mapToObj(i ->
                                new RecommendationItemResponse(
                                        sorted.get(i).productId(),
                                        sorted.get(i).score(),
                                        i + 1
                                )
                        )
                        .toList();

        return new BasicRecommendationResponse(
                recommendationId,
                items
        );
    }
}
