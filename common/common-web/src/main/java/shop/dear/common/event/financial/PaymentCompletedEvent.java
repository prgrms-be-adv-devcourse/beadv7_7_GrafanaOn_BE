package shop.dear.common.event.financial;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        String orderType,
        Long memberId,
        BigDecimal amount,
        OffsetDateTime paidAt
) {
}