package shop.dear.common.event.financial;

import java.math.BigDecimal;

public record PaymentRequestedEvent(
        Long orderId,
        String orderType,
        Long memberId,
        BigDecimal amount
) {
}
