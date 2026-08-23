package shop.dear.identity.auth.authentication.domain;

import java.util.Optional;

public interface AuthAccountRepository {

    AuthAccount save(AuthAccount authAccount);

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findByMemberId(Long memberId);

    Optional<AuthAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmail(String email);
}
