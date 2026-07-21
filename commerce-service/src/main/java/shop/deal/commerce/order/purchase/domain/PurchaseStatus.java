package shop.deal.commerce.order.purchase.domain;

public enum PurchaseStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED,
    PURCHASE_CONFIRMED,
    REFUNDED
}
