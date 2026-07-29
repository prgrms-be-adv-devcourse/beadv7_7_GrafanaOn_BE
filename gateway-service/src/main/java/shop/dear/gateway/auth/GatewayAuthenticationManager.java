package shop.dear.gateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GatewayAuthenticationManager implements ReactiveAuthenticationManager {
    private final GatewayJwtValidator gatewayJwtValidator;

    /**
     * Bearer Access Token
     * 1. GatewayJwtValidator 검증
     * 2. AuthenticatedUser 생성
     * 3. Spring Security Authentication 생성
     * 4. SecurityContext에 저장
     * 권한 규칙을 맞추기 위해 ROLE_ 접두사 필요
     */
    @Override
    public Mono<Authentication> authenticate(
            Authentication authentication) {
        Object credentials = authentication.getCredentials();

        if(!(credentials instanceof String accessToken) || !StringUtils.hasText(accessToken)) {
            return Mono.error(new BadCredentialsException("Access Token이 누락되었습니다."));
        }

        AuthenticatedUser authenticatedUser =
                gatewayJwtValidator.validateAccessToken(accessToken);

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role());

        Authentication authenticatedToken =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        accessToken,
                        List.of(authority)
                );

        return Mono.just(authenticatedToken);
    }
}
