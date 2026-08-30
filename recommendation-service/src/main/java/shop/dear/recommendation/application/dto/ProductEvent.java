package shop.dear.recommendation.application.dto;

import java.time.LocalDateTime;

public record ProductEvent(
	Long eventId,
	String aggregateType,
	String aggregateId,
	String eventType,
	String payload,
	LocalDateTime occurredAt
) {

	private static final String PRODUCT_DELETED = "PRODUCT_DELETED";

	public boolean isDeleted() {
		return PRODUCT_DELETED.equals(this.eventType);
	}

	//과거 이벤트로 최신 데이터를 덮어쓰지 않기 위해 검증
	public boolean isStaleAgainst(final LocalDateTime lastProcessedOccurredAt) {
		return lastProcessedOccurredAt != null && this.occurredAt.isBefore(lastProcessedOccurredAt);
	}
}
