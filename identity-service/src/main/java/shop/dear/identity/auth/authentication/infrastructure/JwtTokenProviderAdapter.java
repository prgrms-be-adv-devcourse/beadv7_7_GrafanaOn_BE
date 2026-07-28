package shop.dear.identity.auth.authentication.infrastructure;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.auth.authentication.application.port.TokenProviderPort;
import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;
import shop.dear.identity.auth.authorization.domain.Role;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {
    // 다음 세 개는 JWT 안에 넣은 Claim의 키
    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtTokenProviderAdapter(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret())
        );
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey) // 서버 비밀 키로 서명했는가
                .requireIssuer(properties.issuer())
                .build();
    }

    @Override
    public TokenResult issueTokens(Long memberId, Role role) {
        Instant issuedAt = Instant.now();
        // 설정한 유효 시간을 현재 시각에 더한다.
        Instant accessTokenExpiresAt = issuedAt.plus(properties.accessTokenExpiration());
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.refreshTokenExpiration());

        String accessToken = createAccessToken(
                memberId,
                role,
                issuedAt,
                accessTokenExpiresAt
        );

        String refreshToken = createRefreshToken(
                memberId,
                issuedAt,
                refreshTokenExpiresAt
        );

        // API 응답용 TokenResult
        return new TokenResult(
                accessToken,
                refreshToken,
                properties.accessTokenExpiration().toSeconds(),
                properties.refreshTokenExpiration().toSeconds()
        );
    }

    private String createAccessToken(
            Long memberId,
            Role role,
            Instant issuedAt,
            Instant expiresAt
    ) {
        // Access Token 생성
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(memberId.toString())
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private String createRefreshToken(Long memberId, Instant issuedAt, Instant expiresAt) {
        // Refresh Token 생성
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(memberId.toString())
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Long parseMemberIdFromRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtParser
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String tokenType = claims.get(
                    TOKEN_TYPE_CLAIM,
                    String.class
            );

            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                throw new BusinessException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN
                );
            }

            Object memberIdClaim = claims.get(MEMBER_ID_CLAIM);

            // Access Token을 재발급 API에 넣는 것을 차단한다.
            if (!(memberIdClaim instanceof Number memberId)) {
                throw new BusinessException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN
                );
            }

            return memberId.longValue();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
