package shop.dear.commerce.financial.payment.application.dto;

import java.math.BigDecimal;

public record ChargeCommand(
        Long memberId,
        BigDecimal amount
) {
}
