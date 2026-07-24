package shop.dear.identity.auth.authentication.application.dto;

// 토큰 재발급 DTO
public record ReissueTokenCommand(
        String refreshToken
) {
}
