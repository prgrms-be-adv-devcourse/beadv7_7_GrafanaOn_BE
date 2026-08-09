package shop.dear.commerce.cart.application.port.dto;

import shop.dear.commerce.order.offer.infrastructure.client.dto.ProductApiData;

import java.math.BigDecimal;

public record CartProductInfo(
    Long productId,
    String name,
    BigDecimal price,
    String thumbnailUrl
) {
    public static CartProductInfo create(ProductApiData data, Long productId) {
        return new CartProductInfo(
                productId,
                data.name(),
                data.price(),
                data.images().isEmpty()
                    ? null
                    : data.images().getFirst().imageUrl()
        );
    }
}
