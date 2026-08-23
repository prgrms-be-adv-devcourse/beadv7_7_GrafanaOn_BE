package shop.dear.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import shop.dear.gateway.auth.BearerTokenAuthenticationConverter;
import shop.dear.gateway.auth.GatewayAuthenticationManager;


/**
 * [공개 API]
 * POST /api/auth/signup
 * POST /api/auth/login
 * POST /api/auth/reissue
 * GET  /api/products
 * GET  /api/search/products
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {
    private final GatewayAuthenticationManager authenticationManager;
    private final BearerTokenAuthenticationConverter authenticationConverter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {
        HttpStatusServerEntryPoint unauthorizedEntryPoint =
                new HttpStatusServerEntryPoint(
                        HttpStatus.UNAUTHORIZED
                );

        AuthenticationWebFilter authenticationWebFilter =
                new AuthenticationWebFilter(authenticationManager);

        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);
        authenticationWebFilter.setSecurityContextRepository(
                NoOpServerSecurityContextRepository.getInstance());
        authenticationWebFilter.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(unauthorizedEntryPoint)
        );

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                unauthorizedEntryPoint
                        )
                        .accessDeniedHandler(
                                new HttpStatusServerAccessDeniedHandler(
                                        HttpStatus.FORBIDDEN
                                )
                        )
                )

                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/reissue"
                        ).permitAll()

                        .pathMatchers(HttpMethod.GET,
                                "/api/products",
                                "/api/search/products"
                        ).permitAll()

                        .pathMatchers(
                                "/api/actuator/health",
                                "/api/actuator/prometheus"
                        ).permitAll()

                        // 내부적으로 forward
                        .pathMatchers("/fallback/**").permitAll()
                        // 구글 로그인 시작과 콜백
                        .pathMatchers(
                                "/api/auth/oauth2/authorization/**",
                                "/api/auth/oauth2/callback/**"
                        ).permitAll()

                        .anyExchange().authenticated()
                )

                .addFilterAt(
                        authenticationWebFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .build();
    }
}
