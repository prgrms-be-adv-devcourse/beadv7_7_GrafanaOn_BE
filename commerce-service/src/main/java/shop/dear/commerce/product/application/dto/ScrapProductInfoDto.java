package shop.dear.commerce.product.application.dto;

import shop.dear.commerce.product.domain.model.Product;

import java.math.BigDecimal;

public record ScrapProductInfoDto(
    Long id,
    String status,
    String imageUrl,
    String brand,
    String name,
    BigDecimal price
) {

    public static ScrapProductInfoDto from(final Product product) {
        return new ScrapProductInfoDto(
            product.getId(),
            product.getStatus().toString(),
            product.getImages().getFirst().getUrl(),
            product.getBrand(),
            product.getName(),
            product.getPrice().getValue()
        );
    }
}
