package shop.dear.common.event.financial;

import java.math.BigDecimal;

public record PaymentReleaseRequestedEvent(
        Long orderId,
        Long memberId,
        BigDecimal amount,
        String reason
) {
}
