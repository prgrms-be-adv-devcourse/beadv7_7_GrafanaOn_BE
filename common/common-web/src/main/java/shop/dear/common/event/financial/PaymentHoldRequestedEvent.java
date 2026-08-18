package shop.dear.common.event.financial;

import java.math.BigDecimal;

public record PaymentHoldRequestedEvent(
        Long orderId,
        Long memberId,
        BigDecimal amount
) {
}
