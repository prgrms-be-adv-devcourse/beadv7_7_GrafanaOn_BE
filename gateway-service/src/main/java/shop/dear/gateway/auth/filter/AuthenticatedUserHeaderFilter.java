package shop.dear.gateway.auth.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import shop.dear.gateway.auth.AuthenticatedUser;

import java.util.Optional;

/**
 * [헤더 위조 방지]
 * X-Authenticated-Member-Id의 값을 우선 제거하고
 * JWT에서 검증된 ID를 넣는다.
 * 공개 API에서 JWT가 없다면 해당 헤더는 제거된 상태로 전달한다.
 */
@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter, Ordered {
    private static final String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Mono<AuthenticatedUser> authenticateUser =
                exchange.getPrincipal()
                        .filter(Authentication.class::isInstance)
                        .map(Authentication.class::cast)
                        .filter(Authentication::isAuthenticated)
                        .map(Authentication::getPrincipal)
                        .filter(AuthenticatedUser.class::isInstance)
                        .map(AuthenticatedUser.class::cast);

        return authenticateUser
                .map(Optional::of)
                .defaultIfEmpty(Optional.<AuthenticatedUser>empty()
                )
                .flatMap(user -> {
                    ServerHttpRequest request =
                            (ServerHttpRequest) exchange.getRequest()
                                    .mutate()
                                    .headers(headers -> {
                                        // 클라이언트가 직접 보낸 값은 무조건 제거한다.
                                        headers.remove(MEMBER_ID_HEADER);

                                        // 검증된 JWT의 memberId만 추가한다.
                                        user.ifPresent(authenticated ->
                                                headers.set(
                                                        MEMBER_ID_HEADER,
                                                        authenticated.memberId().toString()
                                                ));
                                    })
                                    .build();

                    ServerWebExchange mutatedExchange =
                            exchange.mutate()
                                    .request(request)
                                    .build();

                    return chain.filter(mutatedExchange);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
