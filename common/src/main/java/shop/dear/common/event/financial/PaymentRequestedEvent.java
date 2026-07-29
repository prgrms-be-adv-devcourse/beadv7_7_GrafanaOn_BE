package shop.dear.common.event.financial;

import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;

public record PaymentRequestedEvent(
        Long orderId,
        OrderType orderType,
        Long memberId,
        BigDecimal amount
) {
}
