package shop.dear.commerce.product.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductChangedEvent(

    Long id,
    String name,
    String modelNumber,
    String category,
    LocalDate releaseDate,
    BigDecimal price,
    String saleType,
    Long viewCount,
    String description

) {
}
