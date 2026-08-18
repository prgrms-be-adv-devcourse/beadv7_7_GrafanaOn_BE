package shop.dear.identity.member.domain.repository;

import shop.dear.identity.member.domain.model.ArchivedAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface ArchivedAccountRepository {

    void saveAll(List<ArchivedAccount> archivedAccounts);
    void deleteAll(List<ArchivedAccount> archivedAccounts);
    List<ArchivedAccount> findByExpiresAtBefore(LocalDateTime expiresAt);
}
