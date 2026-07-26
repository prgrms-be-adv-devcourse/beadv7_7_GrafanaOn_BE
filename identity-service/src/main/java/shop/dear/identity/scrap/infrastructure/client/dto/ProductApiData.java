package shop.dear.identity.scrap.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductApiData(
    List<ProductSummaryItem> products
) {
    public record ProductSummaryItem(
        Long id,
        String name,
        String brand,
        BigDecimal price,
        String thumbnailUrl,
        String status
    ) {
    }
}
