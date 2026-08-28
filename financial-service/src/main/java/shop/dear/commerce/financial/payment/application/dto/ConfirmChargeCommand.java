package shop.dear.commerce.financial.payment.application.dto;

import java.math.BigDecimal;

public record ConfirmChargeCommand(
        Long memberId,
        String paymentKey,
        String orderId,
        BigDecimal amount
) {
}
