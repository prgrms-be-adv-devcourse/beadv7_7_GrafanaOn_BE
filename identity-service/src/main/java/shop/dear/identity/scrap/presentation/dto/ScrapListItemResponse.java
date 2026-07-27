package shop.dear.identity.scrap.presentation.dto;

import shop.dear.identity.scrap.application.dto.ScrapDetail;

import java.math.BigDecimal;

public record ScrapListItemResponse(
    Long id,
    String name,
    String brand,
    BigDecimal price,
    String imageUrl,
    String status
) {
    public static ScrapListItemResponse from(final ScrapDetail detail) {
        return new ScrapListItemResponse(
            detail.id(),
            detail.name(),
            detail.brand(),
            detail.price(),
            detail.imageUrl(),
            detail.status()
        );
    }
}
