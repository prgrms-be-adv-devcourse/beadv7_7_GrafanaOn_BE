package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.GetProductDto;

import java.math.BigDecimal;

public record GetProductResponse(
    Long id,
    String saleType,
    String status,
    String url,
    String name,
    String brand,
    BigDecimal price,
    Long viewCount
) {

    public static GetProductResponse of(final GetProductDto dto) {
        return new GetProductResponse(
            dto.id(),
            dto.saleType(),
            dto.status(),
            dto.url(),
            dto.name(),
            dto.brand(),
            dto.price(),
            dto.viewCount()
        );
    }
}
