package shop.dear.commerce.financial.wallet.application.dto;

import java.math.BigDecimal;

public record ReleaseCommand(
        Long memberId,
        BigDecimal amount,
        Long offerId
) {
}
