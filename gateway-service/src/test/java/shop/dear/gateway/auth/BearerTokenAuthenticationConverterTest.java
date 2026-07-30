package shop.dear.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bearer Token 정상 추출
 * Authorization Header가 없으면 인증 시도하지 않음
 * Basic 형식 거부
 * Bearer 뒤 Token이 비어 있으면 거부
 */
class BearerTokenAuthenticationConverterTest {

    private BearerTokenAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter =
                new BearerTokenAuthenticationConverter();
    }

    @Test
    void bearerTokenConversionSuccess() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/members/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer access-token"
                                )
                );

        Mono<Authentication> result =
                converter.convert(exchange);

        StepVerifier.create(result)
                .assertNext(authentication -> {
                    assertThat(authentication.isAuthenticated())
                            .isFalse();
                    assertThat(authentication.getCredentials())
                            .isEqualTo("access-token");
                })
                .verifyComplete();
    }

    @Test
    void emptyAuthorizationHeaderReturnsEmpty() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/members/me")
                );

        Mono<Authentication> result =
                converter.convert(exchange);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void nonBearerAuthorizationHeaderIsRejected() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/members/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Basic credentials"
                                )
                );

        Mono<Authentication> result =
                converter.convert(exchange);

        StepVerifier.create(result)
                .expectErrorSatisfies(exception -> {
                    assertThat(exception)
                            .isInstanceOf(
                                    BadCredentialsException.class
                            );
                    assertThat(exception)
                            .hasMessage(
                                    "Authorization Header가 Bearer 형식이 아닙니다."
                            );
                })
                .verify();
    }

    @Test
    void emptyBearerTokenIsRejected() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/members/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer "
                                )
                );

        Mono<Authentication> result =
                converter.convert(exchange);

        StepVerifier.create(result)
                .expectErrorSatisfies(exception -> {
                    assertThat(exception)
                            .isInstanceOf(
                                    BadCredentialsException.class
                            );
                    assertThat(exception)
                            .hasMessage(
                                    "Access Token이 누락되었습니다."
                            );
                })
                .verify();
    }
}