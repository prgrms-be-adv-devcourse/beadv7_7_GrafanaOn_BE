package shop.deal.commerce.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED,
    PURCHASE_CONFIRMED,
    REFUNDED
}
