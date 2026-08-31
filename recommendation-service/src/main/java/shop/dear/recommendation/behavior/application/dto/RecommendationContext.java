package shop.dear.recommendation.behavior.application.dto;

import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.model.UserInterest;

import java.util.List;

public record RecommendationContext(
        List<UserInterest> interests,
        List<RecommendationItem> candidates
) {

    public RecommendationContext {
        interests = List.copyOf(interests);
        candidates = List.copyOf(candidates);
    }

    public static RecommendationContext empty() {
        return new RecommendationContext(
                List.of(),
                List.of()
        );
    }

    public boolean isEmpty() {
        return interests.isEmpty()
                || candidates.isEmpty();
    }
}