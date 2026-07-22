package shop.dear.identity.auth.authentication.presentation.dto;

import shop.dear.identity.auth.authentication.application.dto.TokenResult;

// Token 응답 DTO
public record TokenResponse(
        String accessTokens,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(
                result.accessToken(),
                "Bearer",
                result.accessTokenExpiresInSeconds()
        );
    }
}
