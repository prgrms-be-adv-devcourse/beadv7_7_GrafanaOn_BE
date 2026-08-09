package shop.dear.commerce.order.offer.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.dear.commerce.order.offer.application.dto.CreateOfferCommand;

public record CreateOfferRequest(
        @NotNull @Positive Long snapshotId,
        @NotBlank String title,
        @NotBlank String story,
        @NotBlank String delivery
) {

    public CreateOfferCommand toCommand(final Long writerId) {
        return new CreateOfferCommand(writerId, snapshotId, title, story, delivery);
    }
}
