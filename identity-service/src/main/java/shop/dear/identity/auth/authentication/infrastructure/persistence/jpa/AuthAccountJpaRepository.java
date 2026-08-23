package shop.dear.identity.auth.authentication.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.auth.authentication.domain.AuthAccount;
import shop.dear.identity.auth.authentication.domain.AuthProvider;

import java.util.Optional;

public interface AuthAccountJpaRepository extends JpaRepository<AuthAccount, Long> {
    Optional<AuthAccount> findByEmailIgnoreCase(String email);

    Optional<AuthAccount> findByMemberId(Long memberId);

    Optional<AuthAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmailIgnoreCase(String email);
}
