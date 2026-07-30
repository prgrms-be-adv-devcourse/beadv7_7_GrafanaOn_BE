package shop.dear.commerce.financial.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.dear.commerce.financial.payment.application.dto.ConfirmChargeCommand;

import java.math.BigDecimal;

public record ConfirmChargeRequest(
        @NotBlank
        String paymentKey,

        @NotBlank
        String orderId,

        @NotNull
        @Positive
        BigDecimal amount
) {
    public ConfirmChargeCommand toCommand(final Long memberId) {
        return new ConfirmChargeCommand(
                memberId,
                paymentKey,
                orderId,
                amount
        );
    }
}
