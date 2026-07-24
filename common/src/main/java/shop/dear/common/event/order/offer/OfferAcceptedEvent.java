package shop.dear.common.event.order.offer;

import java.math.BigDecimal;

public record OfferAcceptedEvent(
    Long id,
    Long buyerId,
    Long sellerId,
    Long productId,
    BigDecimal amount,
    String status
) {
}
