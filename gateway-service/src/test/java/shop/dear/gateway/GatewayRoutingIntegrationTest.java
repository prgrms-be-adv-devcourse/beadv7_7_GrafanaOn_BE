package shop.dear.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * 테스트 아에서 가짜 identity 서버를 임시 포트로 실행한다.
 * 클라이언트가 Header에 999 입력
 * → Gateway가 JWT 검증
 * → 조작된 999 제거
 * → JWT의 memberId 1 주입
 * → 가짜 Identity 서버가 받은 값 1 응답
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
class GatewayRoutingIntegrationTest {

    private static final String MEMBER_ID_HEADER =
            "X-Authenticated-Member-Id";

    private static final String ENCODED_SECRET =
            "Z2F0ZXdheS1qd3QtdGVzdC1zZWNyZXQta2V5"
                    + "LW11c3QtYmUtbG9uZy1lbm91Z2g=";

    private static final DisposableServer BACKEND_SERVER =
            HttpServer.create()
                    .port(0)
                    .route(routes -> routes.get(
                            "/api/members/me",
                            (request, response) -> {
                                String memberId =
                                        request.requestHeaders()
                                                .get(
                                                        MEMBER_ID_HEADER
                                                );

                                return response.sendString(
                                        Mono.just(
                                                memberId == null
                                                        ? "missing"
                                                        : memberId
                                        )
                                );
                            }
                    ))
                    .bindNow();

    @DynamicPropertySource
    static void gatewayRouteProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.cloud.gateway.server.webflux"
                        + ".routes[0].id",
                () -> "identity-test-route"
        );

        registry.add(
                "spring.cloud.gateway.server.webflux"
                        + ".routes[0].uri",
                () -> "http://localhost:"
                        + BACKEND_SERVER.port()
        );

        registry.add(
                "spring.cloud.gateway.server.webflux"
                        + ".routes[0].predicates[0]",
                () -> "Path=/api/members/**"
        );
    }

    @LocalServerPort
    private int gatewayPort;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl(
                        "http://localhost:" + gatewayPort
                )
                .build();
    }

    @AfterAll
    static void tearDown() {
        BACKEND_SERVER.disposeNow();
    }

    @Test
    void verifiedMemberIdIsForwardedToDownstream() {
        String accessToken = createAccessToken();

        webTestClient.get()
                .uri("/api/members/me")

                // 클라이언트가 조작한 Header
                .header(
                        MEMBER_ID_HEADER,
                        "999"
                )

                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )

                .exchange()

                .expectStatus()
                .isOk()

                .expectBody(String.class)
                .isEqualTo("1");
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