package shop.dear.commerce.financial.payment.application.event;

import java.math.BigDecimal;

public record WalletDebitRequestedEvent(
        Long paymentId,
        Long memberId,
        BigDecimal amount,
        String orderType
) {
}
