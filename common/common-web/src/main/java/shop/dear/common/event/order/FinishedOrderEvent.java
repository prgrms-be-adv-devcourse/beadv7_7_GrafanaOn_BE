package shop.dear.common.event.order;

import java.math.BigDecimal;

public record FinishedOrderEvent(
    Long orderId,
    Long buyerId,
    Long sellerId,
    Long productId,
    BigDecimal amount,
    String orderType
) {
}
