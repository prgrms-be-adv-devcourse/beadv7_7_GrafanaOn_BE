package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.identity.member.domain.model.ArchivedAccount;

import java.time.LocalDateTime;

public interface ArchivedAccountJpaRepository extends JpaRepository<ArchivedAccount, Long> {

	@Modifying(clearAutomatically = true)
	@Query("delete from ArchivedAccount a where a.expiresAt < :expiresAt")
	int deleteExpired(@Param("expiresAt") LocalDateTime expiresAt);
}
