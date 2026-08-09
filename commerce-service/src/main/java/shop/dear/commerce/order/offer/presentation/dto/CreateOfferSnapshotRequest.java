package shop.dear.commerce.order.offer.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.dear.commerce.order.offer.application.dto.CreateOfferSnapshotCommand;

public record CreateOfferSnapshotRequest(
        @NotNull @Positive Long productId
) {

    public CreateOfferSnapshotCommand toCommand(final Long writerId) {
        return new CreateOfferSnapshotCommand(writerId, productId);
    }
}
