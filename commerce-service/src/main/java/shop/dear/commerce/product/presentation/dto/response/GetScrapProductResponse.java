package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.ScrapProductInfoDto;

import java.math.BigDecimal;

public record GetScrapProductResponse(
    Long id,
    String status,
    String imageUrl,
    String brand,
    String name,
    BigDecimal price
) {

    public static GetScrapProductResponse of(final ScrapProductInfoDto dto) {
        return new GetScrapProductResponse(
            dto.id(),
            dto.status(),
            dto.imageUrl(),
            dto.brand(),
            dto.name(),
            dto.price()
        );
    }
}
