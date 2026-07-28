package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.GetSellerProductDto;

import java.math.BigDecimal;

public record GetSellerProductResponse(
    Long id,
    String status,
    String url,
    String name,
    String brand,
    BigDecimal price,
    Long viewCount
) {

    public static GetSellerProductResponse of(final GetSellerProductDto dto) {
        return new GetSellerProductResponse(
            dto.id(),
            dto.status(),
            dto.url(),
            dto.name(),
            dto.brand(),
            dto.price(),
            dto.viewCount()
        );
    }
}
