package shop.dear.commerce.cart.application.dto;

import java.math.BigDecimal;

public record CartItemDto (
        Long cartItemId,
        Long productId,
        String productName,
        String thumbnailUrl,
        BigDecimal productPrice,
        String status
){
    public static CartItemDto of(
            Long cartItemId,
            Long productId,
            String productName,
            String thumbnailUrl,
            BigDecimal productPrice,
            String status
    ) {
        return new CartItemDto(
                cartItemId,
                productId,
                productName,
                thumbnailUrl,
                productPrice,
                status
        );
    }
}
