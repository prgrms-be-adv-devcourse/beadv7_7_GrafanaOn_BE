package shop.dear.commerce.cart.application.port.dto;

import java.math.BigDecimal;

public record CartProductInfo(
    Long id,
    String name,
    BigDecimal price,
    String thumbnailUrl,
    String status
) {
}
