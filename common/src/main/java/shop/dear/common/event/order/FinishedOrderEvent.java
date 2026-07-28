package shop.dear.common.event.order;

import java.math.BigDecimal;

public record FinishedOrderEvent(
    Long orderId, // Offer 또는 Purchase의 식별자 (OrderType에 따라 Offer 또는 Purchase의 ID를 의미)
    Long buyerId,
    Long sellerId,
    Long productId,
    BigDecimal amount,
    OrderType orderType
) {
}
