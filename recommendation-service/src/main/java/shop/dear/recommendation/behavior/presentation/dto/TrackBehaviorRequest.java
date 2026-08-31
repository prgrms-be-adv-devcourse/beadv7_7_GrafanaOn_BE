package shop.dear.recommendation.behavior.presentation.dto;

import shop.dear.recommendation.behavior.application.dto.TrackBehaviorCommand;
import shop.dear.recommendation.behavior.domain.constant.BehaviorType;

import java.time.LocalDateTime;

public record TrackBehaviorRequest (
    String eventId,
    String recommendationId,
    Long productId,
    BehaviorType eventType,
    LocalDateTime occurredAt
) {
   public TrackBehaviorCommand toCommand() {
       return new TrackBehaviorCommand(
               eventId,
               recommendationId,
               productId,
               eventType,
               occurredAt
       );
   }
}
