package shop.dear.commerce.product.infrastructure.client.dto;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ProductEventApiRequest(
    Long id,
    String aggregateType,
    String aggregateId,
    String eventType,
    JsonNode payload,
    LocalDateTime occurredAt
) {
}
