package shop.dear.commerce.order.offer.application.dto;

public record CreateOfferCommand(
        Long writerId,
        Long snapshotId,
        String title,
        String story,
        String delivery
) {
}
