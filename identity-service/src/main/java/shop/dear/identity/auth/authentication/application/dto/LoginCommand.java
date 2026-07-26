package shop.dear.identity.auth.authentication.application.dto;

// 로그인 요청 DTO
public record LoginCommand(
        String email,
        String rawPassword
) {
}
