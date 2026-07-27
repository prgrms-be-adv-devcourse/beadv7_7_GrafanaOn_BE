package shop.dear.commerce.order.offer.application.dto;

public record CreateOfferSnapshotCommand(
        Long writerId,
        Long productId
) {
}
