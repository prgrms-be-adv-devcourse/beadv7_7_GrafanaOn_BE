package shop.dear.identity.scrap.infrastructure.client.dto;

import java.math.BigDecimal;

public record ProductApiData(
    Long id,
    String name,
    String brand,
    BigDecimal price,
    String imageUrl,
    String status
) {
}
