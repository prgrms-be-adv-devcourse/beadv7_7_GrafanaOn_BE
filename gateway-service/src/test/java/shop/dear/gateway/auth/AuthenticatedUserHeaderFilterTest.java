package shop.dear.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 클라이언트가 999를 보내도 JWT가 1이면 downstream에는 1 전달
 * JWT가 없다면 클라이언트가 보낸 999는 제거
 */
class AuthenticatedUserHeaderFilterTest {

    private static final String MEMBER_ID_HEADER =
            "X-Authenticated-Member-Id";

    private AuthenticatedUserHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticatedUserHeaderFilter();
    }

    @Test
    void verifiedMemberIdReplacesClientHeader() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/members/me")
                                .header(
                                        MEMBER_ID_HEADER,
                                        "999"
                                )
                );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        1L,
                        "BUYER"
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        "access-token",
                        List.of()
                );

        ServerWebExchange authenticatedExchange =
                exchange.mutate()
                        .principal(Mono.just(authentication))
                        .build();

        AtomicReference<String> forwardedMemberId =
                new AtomicReference<>();

        GatewayFilterChain chain = forwardedExchange -> {
            forwardedMemberId.set(
                    forwardedExchange.getRequest()
                            .getHeaders()
                            .getFirst(MEMBER_ID_HEADER)
            );

            return Mono.empty();
        };

        StepVerifier.create(
                        filter.filter(
                                authenticatedExchange,
                                chain
                        )
                )
                .verifyComplete();

        assertThat(forwardedMemberId.get())
                .isEqualTo("1");
    }

    @Test
    void clientHeaderIsRemovedWhenUserIsNotAuthenticated() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/products")
                                .header(
                                        MEMBER_ID_HEADER,
                                        "999"
                                )
                );

        AtomicReference<String> forwardedMemberId =
                new AtomicReference<>();

        GatewayFilterChain chain = forwardedExchange -> {
            forwardedMemberId.set(
                    forwardedExchange.getRequest()
                            .getHeaders()
                            .getFirst(MEMBER_ID_HEADER)
            );

            return Mono.empty();
        };

        StepVerifier.create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        assertThat(forwardedMemberId.get())
                .isNull();
    }
}