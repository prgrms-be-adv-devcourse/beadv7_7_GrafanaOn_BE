package shop.dear.common.event.financial;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        String orderType,
        Long memberId,
        BigDecimal amount
) {
}
