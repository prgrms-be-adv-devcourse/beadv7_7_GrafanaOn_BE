package shop.dear.gateway.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 정상 Access Token 허용
 * Refresh Token 거부
 * 만료된 Access Token 거부
 * 다른 Secret으로 서명한 Token 거부
 */
class GatewayJwtValidatorTest {

    private static final String ISSUER =
            "dear-identity-service";

    private static final String RAW_SECRET =
            "gateway-jwt-test-secret-key-must-be-long-enough";

    private GatewayJwtValidator gatewayJwtValidator;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        String encodedSecret = Encoders.BASE64.encode(
                RAW_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        GatewayJwtProperties properties =
                new GatewayJwtProperties(
                        ISSUER,
                        encodedSecret
                );

        signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(encodedSecret)
        );

        gatewayJwtValidator =
                new GatewayJwtValidator(properties);
    }

    @Test
    void validateAccessTokenSuccess() {
        String accessToken = createToken(
                signingKey,
                ISSUER,
                "ACCESS",
                Instant.now().plusSeconds(600)
        );

        AuthenticatedUser authenticatedUser =
                gatewayJwtValidator.validateAccessToken(
                        accessToken
                );

        assertThat(authenticatedUser.memberId())
                .isEqualTo(1L);
        assertThat(authenticatedUser.role())
                .isEqualTo("BUYER");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = createToken(
                signingKey,
                ISSUER,
                "REFRESH",
                Instant.now().plusSeconds(600)
        );

        assertThatThrownBy(() ->
                gatewayJwtValidator.validateAccessToken(
                        refreshToken
                )
        )
                .isInstanceOf(
                        BadCredentialsException.class
                )
                .hasMessage(
                        "Access Token이 아닙니다."
                );
    }

    @Test
    void expiredAccessTokenIsRejected() {
        String expiredToken = createToken(
                signingKey,
                ISSUER,
                "ACCESS",
                Instant.now().minusSeconds(1)
        );

        assertThatThrownBy(() ->
                gatewayJwtValidator.validateAccessToken(
                        expiredToken
                )
        )
                .isInstanceOf(
                        CredentialsExpiredException.class
                )
                .hasMessage(
                        "만료된 Access Token입니다."
                );
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        String otherSecret = Encoders.BASE64.encode(
                "different-gateway-jwt-test-secret-key-value"
                        .getBytes(StandardCharsets.UTF_8)
        );

        SecretKey otherSigningKey =
                Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(otherSecret)
                );

        String accessToken = createToken(
                otherSigningKey,
                ISSUER,
                "ACCESS",
                Instant.now().plusSeconds(600)
        );

        assertThatThrownBy(() ->
                gatewayJwtValidator.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(
                        BadCredentialsException.class
                )
                .hasMessage(
                        "유효하지 않은 Access Token입니다."
                );
    }

    private String createToken(
            SecretKey key,
            String issuer,
            String tokenType,
            Instant expiresAt
    ) {
        Instant issuedAt = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject("1")
                .claim("memberId", 1L)
                .claim("role", "BUYER")
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}