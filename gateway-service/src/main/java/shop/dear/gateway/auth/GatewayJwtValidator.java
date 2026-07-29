package shop.dear.gateway.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;

/**
 * Identity와 같은 Secret으로 서명되었는가?
 * 발급자 issuer가 일치하는가?
 * 만료되지 않은 Token인가?
 * Refresh Token이 아닌 Access Token인가?
 * memberId와 role이 유효한 값으로 이루어져 있는가?
 * JWT의 subject와 memberId가 일치하는가?
 */
@Component
public class GatewayJwtValidator {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    private final JwtParser jwtParser;

    public GatewayJwtValidator(GatewayJwtProperties properties) {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret())
        );

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build();
    }

    public AuthenticatedUser validateAccessToken(
            String accessToken
    ) {
        try {
            Claims claims = jwtParser
                    .parseSignedClaims(accessToken)
                    .getPayload();

            validateTokenType(claims);

            Long memberId = extractMemberId(claims);
            String role = extractRole(claims);

            validateSubject(claims, memberId);

            return new AuthenticatedUser(memberId, role);
        } catch (ExpiredJwtException exception) {
            throw new CredentialsExpiredException("만료된 Access Token입니다.", exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("유효하지 않은 Access Token입니다.", exception);
        }
    }

    private void validateTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if(!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new BadCredentialsException("Access Token 형식이 아닙니다.");
        }
    }

    private Long extractMemberId(Claims claims) {
        Object memberIdClaim = claims.get(MEMBER_ID_CLAIM);

        if (!(memberIdClaim instanceof Number memberId)) {
            throw new BadCredentialsException("회원 ID가 누락된 Access Token입니다.");
        }

        long value = memberId.longValue();

        if (value <= 0) {
            throw new BadCredentialsException("유효하지 않은 회원 ID입니다.");
        }

        return value;
    }

    private String extractRole(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);

        if (!StringUtils.hasText(role)) {
            throw new BadCredentialsException("권한 정보가 누락된 Access Token입니다.");
        }

        return role;
    }

    private void validateSubject(Claims claims, Long memberId) {
        if (!memberId.toString().equals(claims.getSubject())) {
            throw new BadCredentialsException("회원 식별 정보가 일치하지 않습니다.");
        }
    }
}
