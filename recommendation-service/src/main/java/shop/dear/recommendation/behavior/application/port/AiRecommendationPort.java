package shop.dear.recommendation.behavior.application.port;

import shop.dear.recommendation.behavior.application.dto.AiRecommendationResult;

import java.util.Optional;

public interface AiRecommendationPort {

    Optional<AiRecommendationResult> recommendSimilar(
            Long baseProductId,
            int limit
    );
}