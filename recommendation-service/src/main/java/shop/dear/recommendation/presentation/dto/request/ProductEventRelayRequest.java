package shop.dear.recommendation.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import shop.dear.recommendation.infrastructure.inbox.InboxEventType;
import shop.dear.recommendation.infrastructure.inbox.RecommendationInbox;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ProductEventRelayRequest(
    @NotNull Long id,
    @NotBlank String aggregateType,
    @NotBlank String aggregateId,
    @NotNull InboxEventType eventType,
    @NotNull JsonNode payload,
    @NotNull LocalDateTime occurredAt
) {
    public RecommendationInbox toInbox() {
        return RecommendationInbox.of(
            id,
            aggregateType,
            aggregateId,
            eventType,
            payload.toString(),
            occurredAt
        );
    }
}
