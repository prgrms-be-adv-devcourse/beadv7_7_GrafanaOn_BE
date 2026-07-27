package shop.dear.commerce.order.purchase.application.port.dto;

import java.math.BigDecimal;

public record ProductInfo(
    Long productId,
    Long sellerId,
    BigDecimal price,
    ProductSaleType saleType,
    ProductStatus status
) {
}
