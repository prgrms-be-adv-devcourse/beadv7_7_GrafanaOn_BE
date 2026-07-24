package shop.dear.identity.auth.authentication.application.port;

import java.time.Instant;

public interface RefreshTokenStorePort {

    void save(Long memberId, String refreshToken, Instant expiresAt);

    boolean matches(Long memberId, String refreshToken);

    void deleteByMemberId(Long memberId);
}
