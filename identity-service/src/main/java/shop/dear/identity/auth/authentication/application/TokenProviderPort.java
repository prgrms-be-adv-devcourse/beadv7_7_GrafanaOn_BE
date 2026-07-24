package shop.dear.identity.auth.authentication.application;

import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authorization.domain.Role;

public interface TokenProviderPort {

    // Access Token과 Refresh Token 발급
    TokenResult issueTokens(Long memberId, Role role);

    // Refresh Token의 서명, 만료, 토큰 종류 검증 후 memberId 반환
    Long parseMemberIdFromRefreshToken(String refreshToken);
}
