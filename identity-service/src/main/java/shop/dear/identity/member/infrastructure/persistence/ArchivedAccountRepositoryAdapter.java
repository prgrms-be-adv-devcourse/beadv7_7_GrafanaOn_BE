package shop.dear.identity.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.identity.member.domain.model.ArchivedAccount;
import shop.dear.identity.member.domain.repository.ArchivedAccountRepository;
import shop.dear.identity.member.infrastructure.persistence.jpa.ArchivedAccountJpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ArchivedAccountRepositoryAdapter implements ArchivedAccountRepository {

    private final ArchivedAccountJpaRepository jpaRepository;

    @Override
    public void saveAll(List<ArchivedAccount> archivedAccounts) {
        jpaRepository.saveAll(archivedAccounts);
    }

    @Override
    public long deleteExpired(LocalDateTime expiresAt) {
        return jpaRepository.deleteExpired(expiresAt);
    }

}
