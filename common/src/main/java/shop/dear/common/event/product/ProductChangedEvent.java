package shop.dear.common.event.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductChangedEvent(

    Long id,
    String name,
    String modelNumber,
    String category,
    LocalDate releaseDate,
    BigDecimal price,
    String saleType,
    Long viewCount,
    String description,
    String fullStory,
    LocalDateTime insertedAt
) {
}
