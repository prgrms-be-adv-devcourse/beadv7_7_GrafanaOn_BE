package shop.dear.commerce.financial.wallet.application.dto;

import java.math.BigDecimal;

public record HoldCommand(
        Long memberId,
        BigDecimal amount,
        Long offerId
) {
}