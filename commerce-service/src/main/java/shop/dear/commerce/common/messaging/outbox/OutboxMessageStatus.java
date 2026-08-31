package shop.dear.commerce.common.messaging.outbox;

public enum OutboxMessageStatus {
    PENDING,
    PUBLISHED,
    DLQ
}
