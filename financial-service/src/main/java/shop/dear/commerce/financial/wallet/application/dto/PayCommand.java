package shop.dear.commerce.financial.wallet.application.dto;

import java.math.BigDecimal;

public record PayCommand(
        Long memberId,
        BigDecimal amount,
        Long paymentId
) {
}
