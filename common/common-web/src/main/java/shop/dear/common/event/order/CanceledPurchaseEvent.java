package shop.dear.common.event.order;

public record CanceledPurchaseEvent(
    Long purchaseId,
    Long buyerId,
    Long sellerId,
    Long productId,
    ReleaseReason reason
) {
}
