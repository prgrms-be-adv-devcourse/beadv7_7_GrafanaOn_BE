package shop.dear.commerce.product.application.dto;

import shop.dear.commerce.product.domain.model.Product;

import java.math.BigDecimal;

public record GetProductDto(
    Long id,
    String saleType,
    String status,
    String url,
    String name,
    String brand,
    BigDecimal price,
    Long viewCount
) {

    public static GetProductDto of(final Product product) {
        return new GetProductDto(
            product.getId(),
            product.getSaleType().toString(),
            product.getStatus().toString(),
            product.getImages().getFirst().getUrl(),
            product.getName(),
            product.getBrand(),
            product.getPrice().getValue(),
            product.getViewCount()
        );
    }
}
