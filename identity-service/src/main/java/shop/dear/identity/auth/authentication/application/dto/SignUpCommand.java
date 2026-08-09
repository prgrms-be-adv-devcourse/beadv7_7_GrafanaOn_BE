package shop.dear.identity.auth.authentication.application.dto;

// 회원가입 DTO
public record SignUpCommand(
        String email,
        String rawPassword,
        String name,
        String defaultShippingAddress,
        String phoneNumber
) {
}
