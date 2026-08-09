package shop.dear.commerce.cart.infrastructure.client.dto;

import shop.dear.commerce.order.offer.infrastructure.client.dto.ProductImageData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CartProductApiData(
        Long sellerId,
        List<ProductImageData> images,
        String name,
        String brand,
        BigDecimal price,
        String modelNumber,
        String category,
        LocalDate releaseDate,
        Long viewCount,
        String description,
        LocalDateTime insertedAt
) {
}
