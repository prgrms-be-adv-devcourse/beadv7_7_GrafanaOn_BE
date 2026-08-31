package shop.dear.recommendation.behavior.application.dto;

import shop.dear.recommendation.behavior.domain.constant.BehaviorType;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.presentation.dto.TrackBehaviorRequest;

import java.time.LocalDateTime;

public record TrackBehaviorCommand(
        String eventId,
        String recommendationId,
        Long productId,
        BehaviorType eventType,
        LocalDateTime occurredAt
) {
}