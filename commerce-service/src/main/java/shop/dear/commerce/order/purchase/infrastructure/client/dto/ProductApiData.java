package shop.dear.commerce.order.purchase.infrastructure.client.dto;

import shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;

import java.math.BigDecimal;

public record ProductApiData(
    Long id,
    Long sellerId,
    BigDecimal price,
    ProductSaleType saleType,
    ProductStatus status
) {}
