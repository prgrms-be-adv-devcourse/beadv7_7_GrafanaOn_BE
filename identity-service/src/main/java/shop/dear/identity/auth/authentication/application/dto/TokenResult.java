package shop.dear.identity.auth.authentication.application.dto;

// 로그인 성공 결과 DTO, null이 절대 불가이므로 long 이용.
public record TokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds
) {
}
