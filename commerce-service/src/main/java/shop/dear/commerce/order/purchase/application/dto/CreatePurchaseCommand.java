package shop.dear.commerce.order.purchase.application.dto;

public record CreatePurchaseCommand(
    Long buyerId,
    Long productId,
    String delivery
) {
}
