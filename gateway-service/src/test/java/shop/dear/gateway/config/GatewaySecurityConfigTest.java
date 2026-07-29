package shop.dear.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import shop.dear.gateway.GatewayApplication;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 검증 실패 → 401
 * JWT 검증 성공 → Security 통과 → Route가 없어서 404 (인증 필터는 정상 통과)
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "auth.jwt.issuer=dear-identity-service",
                "auth.jwt.secret="
                        + "Z2F0ZXdheS1qd3QtdGVzdC1zZWNyZXQta2V5"
                        + "LW11c3QtYmUtbG9uZy1lbm91Z2g="
        }
)
class GatewaySecurityConfigTest {

    private static final String ENCODED_SECRET =
            "Z2F0ZXdheS1qd3QtdGVzdC1zZWNyZXQta2V5"
                    + "LW11c3QtYmUtbG9uZy1lbm91Z2g=";

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void healthCheckIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void protectedApiWithoutTokenReturnsUnauthorized() {
        webTestClient.get()
                .uri("/api/members/me")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void protectedApiWithInvalidTokenReturnsUnauthorized() {
        webTestClient.get()
                .uri("/api/members/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer invalid-token"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized(); // 401
    }

    @Test
    void validAccessTokenPassesAuthentication() {
        String accessToken = createAccessToken();

        webTestClient.get()
                .uri("/api/members/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isNotFound(); // Route가 없어 404
    }

    private String createAccessToken() {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        ENCODED_SECRET
                )
        );

        Instant issuedAt = Instant.now();
        Instant expiresAt =
                issuedAt.plusSeconds(600);

        return Jwts.builder()
                .issuer("dear-identity-service")
                .subject("1")
                .claim("memberId", 1L)
                .claim("role", "BUYER")
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(
                        signingKey,
                        Jwts.SIG.HS256
                )
                .compact();
    }
}