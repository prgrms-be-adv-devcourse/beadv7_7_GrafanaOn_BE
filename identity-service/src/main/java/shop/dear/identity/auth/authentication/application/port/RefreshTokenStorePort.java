package shop.dear.identity.auth.authentication.application.port;

import shop.dear.identity.auth.authentication.application.dto.RefreshTokenVerificationResult;

import java.time.Instant;

public interface RefreshTokenStorePort {

    void save(Long memberId, String refreshToken, Instant expiresAt);

    RefreshTokenVerificationResult verify(Long memberId, String refreshToken);

    void deleteByMemberId(Long memberId);

    // 재사용 탐지 시 별도 트랜잭션으로 토큰을 폐기할 전용 포트
    void revokeCompromisedSession(Long memberId);
}
