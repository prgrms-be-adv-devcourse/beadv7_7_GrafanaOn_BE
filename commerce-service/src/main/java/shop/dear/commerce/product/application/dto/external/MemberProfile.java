package shop.dear.commerce.product.application.dto.external;

public record MemberProfile(
    String name,
    String nickname,
    String email,
    String defaultShippingAddress,
    String phoneNumber
) {
}
