package shop.dear.common.event.order;

public record CanceledPurchaseEvent(
    Long purchaseId,
    Long buyerId,
    Long sellerId,
    Long productId,
    String reason
) {
}
