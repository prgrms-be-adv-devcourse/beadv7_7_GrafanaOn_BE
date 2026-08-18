package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.model.ArchivedAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface ArchivedAccountJpaRepository extends JpaRepository<ArchivedAccount, Long> {
	List<ArchivedAccount> findByExpiresAtBefore(LocalDateTime expiresAt);
}
