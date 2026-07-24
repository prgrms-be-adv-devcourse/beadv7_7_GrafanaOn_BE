package shop.dear.identity.auth.authentication.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.identity.auth.authentication.domain.AuthAccount;
import shop.dear.identity.auth.authentication.domain.AuthAccountRepository;
import shop.dear.identity.auth.authentication.infrastructure.persistence.jpa.AuthAccountJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthAccountRepositoryAdapter implements AuthAccountRepository {
    private final AuthAccountJpaRepository jpaRepository;

    @Override
    public AuthAccount save(AuthAccount authAccount) {
        return jpaRepository.save(authAccount);
    }

    @Override
    public Optional<AuthAccount> findByEmail(String email) {
        return jpaRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Optional<AuthAccount> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }
}
