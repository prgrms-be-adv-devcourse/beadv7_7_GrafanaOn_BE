package shop.dear.recommendation.behavior.application.dto;

import shop.dear.recommendation.behavior.domain.constant.BehaviorType;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.presentation.dto.TrackBehaviorRequest;

import java.time.LocalDateTime;

public record TrackBehaviorCommand(
    String eventId,
    String recommendationId,
    Long memberId,
    Long productId,
    BehaviorType eventType,
    LocalDateTime occurredAt
) {
    public static TrackBehaviorCommand from(final TrackBehaviorRequest request) {
        return new TrackBehaviorCommand(
            request.eventId(),
                request.recommendationId(),
                request.memberId(),
                request.productId(),
                request.eventType(),
                request.occurredAt()
        );
    }
}
