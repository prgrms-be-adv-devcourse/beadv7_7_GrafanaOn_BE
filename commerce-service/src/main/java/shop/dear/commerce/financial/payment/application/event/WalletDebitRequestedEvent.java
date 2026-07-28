package shop.dear.commerce.financial.payment.application.event;

import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;

public record WalletDebitRequestedEvent(
        Long paymentId,
        Long memberId,
        BigDecimal amount,
        OrderType orderType
) {
}
