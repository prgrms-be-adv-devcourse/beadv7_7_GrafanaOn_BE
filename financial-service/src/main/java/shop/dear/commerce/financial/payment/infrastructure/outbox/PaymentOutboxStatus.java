package shop.dear.commerce.financial.payment.infrastructure.outbox;

public enum PaymentOutboxStatus {
    PENDING,
    SENT,
    FAILED,
    EXHAUSTED
}
