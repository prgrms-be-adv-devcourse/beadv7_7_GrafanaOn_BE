package shop.dear.commerce.financial.settlement.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;

import java.time.LocalDateTime;
import java.util.List;

public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

	@Query("""
        SELECT s FROM Settlement s
        WHERE s.insertedAt >= :startDate
          AND s.insertedAt < :endDate
	      AND s.walletId = :walletId
          AND s.state = :state
	  """)
	List<Settlement> findByInsertedAtBetween(
		@Param("startDate")LocalDateTime startDate,
		@Param("endDate")LocalDateTime endDate,
		@Param("walletId")Long walletId,
		@Param("state") SettlementStatus state
	);

	// 정산 대상 지갑 목록 (지갑 단위로 나눠서 처리하기 위함)
	@Query("""
        SELECT DISTINCT s.walletId FROM Settlement s
        WHERE s.insertedAt >= :startDate
          AND s.insertedAt < :endDate
          AND s.state = :state
	  """)
	List<Long> findWalletIdsByInsertedAtBetween(
		@Param("startDate")LocalDateTime startDate,
		@Param("endDate")LocalDateTime endDate,
		@Param("state") SettlementStatus state
	);
}
