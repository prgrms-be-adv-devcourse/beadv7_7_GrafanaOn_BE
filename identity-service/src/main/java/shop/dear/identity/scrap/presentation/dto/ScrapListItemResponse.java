package shop.dear.identity.scrap.presentation.dto;

import shop.dear.identity.scrap.application.dto.ScrapDetail;

import java.math.BigDecimal;

public record ScrapListItemResponse(
    Long productId,
    String productName,
    String brand,
    BigDecimal price,
    String thumbnailUrl,
    String productStatus
) {
    public static ScrapListItemResponse from(final ScrapDetail detail) {
        return new ScrapListItemResponse(
            detail.productId(),
            detail.productName(),
            detail.brand(),
            detail.price(),
            detail.thumbnailUrl(),
            detail.productStatus()
        );
    }
}
