package shop.dear.identity.scrap.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSummariesApiData(
    List<ProductSummaryItem> products
) {
    public record ProductSummaryItem(
        Long productId,
        String name,
        String brand,
        BigDecimal price,
        String thumbnailUrl,
        String status
    ) {
    }
}
