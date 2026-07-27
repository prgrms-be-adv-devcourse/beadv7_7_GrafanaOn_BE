package shop.dear.commerce.order.purchase.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;

public record CreatePurchaseRequest(
    @NotNull @Positive Long productId,
    @NotBlank @Size(max = 255) String delivery
) {

    public CreatePurchaseCommand toCommand(final Long buyerId) {
        return new CreatePurchaseCommand(buyerId, productId, delivery);
    }
}
