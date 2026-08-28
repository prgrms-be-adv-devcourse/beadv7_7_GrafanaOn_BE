package shop.dear.commerce.financial.payment.application.event;

import java.math.BigDecimal;

public record WalletTopUpRequestedEvent(
        Long paymentId,
        Long memberId,
        BigDecimal amount
) {
}
