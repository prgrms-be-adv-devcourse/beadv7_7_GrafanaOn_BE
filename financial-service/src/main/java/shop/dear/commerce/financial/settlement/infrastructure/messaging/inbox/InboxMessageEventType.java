package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

// settlement 가 수신하는 Stream 이벤트 유형
public enum InboxMessageEventType {

	ORDER_FINISHED("FinishedOrderEvent");

	private final String publishedName;

	InboxMessageEventType(final String publishedName) {
		this.publishedName = publishedName;
	}

	public String publishedName() {
		return publishedName;
	}

	public boolean matches(final String eventType) {
		return this.publishedName.equals(eventType);
	}
}
