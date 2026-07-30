package shop.dear.commerce.cart.infrastructure.event;

public record CartItemAddRequestedEvent (
        Long memberId,
        Long productid
) {
}
