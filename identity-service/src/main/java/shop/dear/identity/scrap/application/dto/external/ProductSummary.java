package shop.dear.identity.scrap.application.dto.external;

import java.math.BigDecimal;

public record ProductSummary(
    Long productId,
    String name,
    String brand,
    BigDecimal price,
    String thumbnailUrl,
    String status
) {
}
