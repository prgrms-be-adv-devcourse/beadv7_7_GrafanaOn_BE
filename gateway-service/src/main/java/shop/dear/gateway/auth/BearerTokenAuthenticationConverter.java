package shop.dear.gateway.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * HTTP Header에서 Bearer Token을 추출하는 Converter
 * 아직 인증되지 않은 Authentication을 생성하고
 * GatewayAuthenticationManager로 전달한다.
 */
public class BearerTokenAuthenticationConverter implements ServerAuthenticationConverter {
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if(!StringUtils.hasText(authorization)) {
            return Mono.empty();
        }

        if(!authorization.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length())) {
            return Mono.error(new BadCredentialsException(
                    "Authorization Header가 Bearer 형식이 아닙니다."
            ));
        }

        String accessToken = authorization
                .substring(BEARER_PREFIX.length())
                .trim();

        if(!StringUtils.hasText(accessToken)) {
            return Mono.error(new BadCredentialsException("Access Token이 누락되었습니다."));
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(null, accessToken);

        return Mono.just(authentication);
    }
}
