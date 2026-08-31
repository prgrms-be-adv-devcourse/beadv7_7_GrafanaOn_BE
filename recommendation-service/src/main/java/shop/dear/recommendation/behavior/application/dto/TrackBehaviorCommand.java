package shop.dear.recommendation.behavior.application.dto;

import shop.dear.recommendation.behavior.domain.constant.BehaviorType;

import java.time.LocalDateTime;

public record TrackBehaviorCommand(
        String eventId,
        String recommendationId,
        Long productId,
        BehaviorType eventType,
        LocalDateTime occurredAt
) {
}