package shop.dear.commerce.order.purchase.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductApiData(
        Long sellerId,
        List<ProductImageData> images,
        String name,
        String brand,
        BigDecimal price,
        String modelNumber,
        String category,
        LocalDate releaseDate,
        String saleType,
        String status,
        Long viewCount,
        String description,
        LocalDateTime insertedAt
) {}
