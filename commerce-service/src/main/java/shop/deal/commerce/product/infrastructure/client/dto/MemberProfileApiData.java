package shop.deal.commerce.product.infrastructure.client.dto;

public record MemberProfileApiData(
    String name,
    String nickname,
    String email,
    String defaultShippingAddress,
    String phoneNumber
) {
}
