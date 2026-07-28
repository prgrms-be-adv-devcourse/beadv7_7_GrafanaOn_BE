package shop.dear.identity.auth.authentication.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;
import shop.dear.identity.auth.authorization.domain.Role;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderAdapterTest {

    private static final String TEST_SECRET =
            "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LW11c3QtYmUtMzItYnl0ZXMtbG9uZw==";

    private JwtTokenProviderAdapter tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-identity-service",
                TEST_SECRET,
                Duration.ofMinutes(5),
                Duration.ofHours(1)
        );

        tokenProvider =
                new JwtTokenProviderAdapter(properties);
    }

    // JWT 발급 검증 시스쳄
    @Test
    @DisplayName("Access Token과 Refresh Token을 발급한다")
    void issueTokensSuccess() {
        TokenResult result = tokenProvider.issueTokens(
                1L,
                Role.BUYER
        );

        assertFalse(result.accessToken().isBlank());
        assertFalse(result.refreshToken().isBlank());
        assertEquals(300L, result.accessTokenExpiresInSeconds());
        assertEquals(3600L, result.refreshTokenExpiresInSeconds());
    }

    @Test
    @DisplayName("Refresh Token에서 memberId를 추출한다")
    void parseMemberIdFromRefreshTokenSuccess() {
        TokenResult result = tokenProvider.issueTokens(
                1L,
                Role.BUYER
        );

        Long memberId =
                tokenProvider.parseMemberIdFromRefreshToken(
                        result.refreshToken()
                );

        assertEquals(1L, memberId);
    }

    @Test
    @DisplayName("Access Token을 재발급용 토큰으로 사용할 수 없다")
    void accessTokenCannotBeUsedAsRefreshToken() {
        TokenResult result = tokenProvider.issueTokens(
                1L,
                Role.BUYER
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> tokenProvider.parseMemberIdFromRefreshToken(
                        result.accessToken()
                )
        );

        assertSame(
                AuthErrorCode.INVALID_REFRESH_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("변조된 JWT는 사용할 수 없다")
    void modifiedTokenIsRejected() {
        TokenResult result = tokenProvider.issueTokens(
                1L,
                Role.BUYER
        );

        String modifiedToken =
                result.refreshToken() + "modified";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> tokenProvider.parseMemberIdFromRefreshToken(
                        modifiedToken
                )
        );

        assertSame(
                AuthErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );
    }
}
