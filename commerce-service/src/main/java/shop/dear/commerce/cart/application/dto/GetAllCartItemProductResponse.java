package shop.dear.commerce.cart.application.dto;

import java.util.List;

public record GetAllCartItemProductResponse(
        Long cartId,
        List<CartItemDto> items
) {
    public static GetAllCartItemProductResponse of(Long cartId, List<CartItemDto> items) {
        return new GetAllCartItemProductResponse(cartId, items);
    }
}
