package shop.dear.commerce.order.offer.application.port.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductInfo(
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
        LocalDateTime insertedAt
) {
}
