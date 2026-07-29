package shop.dear.common.event.financial;

import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        OrderType orderType,
        Long memberId,
        BigDecimal amount
) {
}
