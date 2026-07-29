package shop.dear.commerce.financial.payment.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.dear.commerce.financial.payment.application.dto.ChargeCommand;

import java.math.BigDecimal;

public record ChargeRequest(
        @NotNull
        @Positive
        BigDecimal amount
) {
    public ChargeCommand toCommand(final Long memberId) {
        return new ChargeCommand(memberId, amount);
    }
}
