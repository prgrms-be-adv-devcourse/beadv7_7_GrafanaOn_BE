package shop.dear.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Access Token
 * → Validator 호출
 * → AuthenticatedUser
 * → ROLE_BUYER 권한을 가진 Authentication
 */
@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationManagerTest {

    @Mock
    private GatewayJwtValidator gatewayJwtValidator;

    private GatewayAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager =
                new GatewayAuthenticationManager(
                        gatewayJwtValidator
                );
    }

    @Test
    void authenticationSuccess() {
        Authentication requestAuthentication =
                new UsernamePasswordAuthenticationToken(
                        null,
                        "access-token"
                );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        1L,
                        "BUYER"
                );

        given(
                gatewayJwtValidator.validateAccessToken(
                        "access-token"
                )
        ).willReturn(authenticatedUser);

        Mono<Authentication> result =
                authenticationManager.authenticate(
                        requestAuthentication
                );

        StepVerifier.create(result)
                .assertNext(authentication -> {
                    assertThat(authentication.isAuthenticated())
                            .isTrue();

                    assertThat(authentication.getPrincipal())
                            .isEqualTo(authenticatedUser);

                    assertThat(authentication.getAuthorities())
                            .extracting(
                                    authority ->
                                            authority.getAuthority()
                            )
                            .containsExactly("ROLE_BUYER");
                })
                .verifyComplete();

        verify(gatewayJwtValidator)
                .validateAccessToken("access-token");
    }

    @Test
    void missingAccessTokenIsRejected() {
        Authentication requestAuthentication =
                new UsernamePasswordAuthenticationToken(
                        null,
                        null
                );

        Mono<Authentication> result =
                authenticationManager.authenticate(
                        requestAuthentication
                );

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

        verifyNoInteractions(gatewayJwtValidator);
    }
}