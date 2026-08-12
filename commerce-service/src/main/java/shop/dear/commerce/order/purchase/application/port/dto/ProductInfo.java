package shop.dear.commerce.order.purchase.application.port.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType.IMMEDIATE;
import static shop.dear.commerce.order.purchase.application.port.dto.ProductStatus.ON_SALE;

public record ProductInfo(
        Long sellerId,
        List<String> images,
        String name,
        String brand,
        BigDecimal price,
        String modelNumber,
        String category,
        LocalDate releaseDate,
        ProductSaleType saleType,
        ProductStatus status,
        Long viewCount,
        String description,
        LocalDateTime insertedAt
) {

  public boolean isOwnedBy(Long buyerId) {
    return Objects.equals(buyerId, sellerId);
  }

  public boolean isOnSale() {
    return status == ON_SALE;
  }

  public boolean isImmediateSale() {
    return saleType == IMMEDIATE;
  }
}
