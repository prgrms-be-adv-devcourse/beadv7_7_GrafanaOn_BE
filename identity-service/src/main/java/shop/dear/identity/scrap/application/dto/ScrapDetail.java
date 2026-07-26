package shop.dear.identity.scrap.application.dto;

import shop.dear.identity.scrap.application.dto.external.ProductSummary;
import shop.dear.identity.scrap.domain.model.Scrap;

import java.math.BigDecimal;

public record ScrapDetail(
    Long id,
    String name,
    String brand,
    BigDecimal price,
    String imageUrl,
    String status
) {
    public static ScrapDetail of(final Scrap scrap, final ProductSummary product) {

        return new ScrapDetail(
            scrap.getProductId(),
            product.name(),
            product.brand(),
            product.price(),
            product.imageUrl(),
            product.status()
        );
    }
}
