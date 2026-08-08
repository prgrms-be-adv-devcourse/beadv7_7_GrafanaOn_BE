package shop.dear.common.event.financial;

import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        OrderType orderType,
        Long memberId,
        BigDecimal amount,
        OffsetDateTime paidAt
) {
}