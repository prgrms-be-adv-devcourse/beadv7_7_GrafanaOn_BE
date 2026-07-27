package shop.dear.commerce.order.purchase.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductApiData(
        Long sellerId,
        List<String> images,
        String name,
        String brand,
        BigDecimal price,
        String modelNumber,
        String category,
        LocalDate releaseDate,
        Long viewCount,
        String description,
        OffsetDateTime insertedAt
) {
}
