package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.identity.member.domain.model.ArchivedAccount;

import java.time.LocalDateTime;

public interface ArchivedAccountJpaRepository extends JpaRepository<ArchivedAccount, Long> {

	/**
	 * 만료된 계좌정보를 청크 단위로 벌크 삭제한다.
	 * 파생 삭제 쿼리(deleteBy...)는 엔티티를 전부 조회한 뒤 1건씩 삭제하므로 사용하지 않는다.
	 * JPQL 서브쿼리는 limit을 지원하지 않아 네이티브 쿼리로 작성했다.
	 */
	@Modifying(clearAutomatically = true)
	@Query(
		value = """
			delete from archived_account
			where id in (
			    select id
			    from archived_account
			    where expires_at < :expiresAt
			    order by id
			    limit :chunkSize
			)
			""",
		nativeQuery = true
	)
	int deleteExpiredChunk(
		@Param("expiresAt") LocalDateTime expiresAt,
		@Param("chunkSize") int chunkSize
	);
}
