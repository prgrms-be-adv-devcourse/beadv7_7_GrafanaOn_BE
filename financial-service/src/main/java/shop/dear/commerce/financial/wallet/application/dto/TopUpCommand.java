package shop.dear.commerce.financial.wallet.application.dto;

import java.math.BigDecimal;

public record TopUpCommand(
        Long memberId,
        BigDecimal amount,
        Long paymentId
) {
}