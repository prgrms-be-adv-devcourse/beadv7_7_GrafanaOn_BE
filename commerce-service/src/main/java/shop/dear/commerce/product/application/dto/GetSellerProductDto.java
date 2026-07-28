package shop.dear.commerce.product.application.dto;

import shop.dear.commerce.product.domain.model.Product;

import java.math.BigDecimal;

public record GetSellerProductDto(
    Long id,
    String status,
    String url,
    String name,
    String brand,
    BigDecimal price,
    Long viewCount
) {

    public static GetSellerProductDto of(final Product product) {
        return new GetSellerProductDto(
            product.getId(),
            product.getStatus().toString(),
            product.getImages().getFirst().getUrl(),
            product.getName(),
            product.getBrand(),
            product.getPrice().getValue(),
            product.getViewCount()
        );
    }
}
