package shop.dear.commerce.financial.settlement.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementSummary;

import java.time.LocalDateTime;
import java.util.List;

public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

	boolean existsByPurchaseIdOrOfferId(Long purchaseId, Long offerId);

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

	// 특정 기간, 특정 상태의 정산 건 전체
	@Query("""
        SELECT s FROM Settlement s
        WHERE s.insertedAt >= :startDate
          AND s.insertedAt < :endDate
          AND s.state = :state
	  """)
	List<Settlement> findAllByInsertedAtBetween(
		@Param("startDate")LocalDateTime startDate,
		@Param("endDate")LocalDateTime endDate,
		@Param("state") SettlementStatus state
	);

	@Query("""
        SELECT new shop.dear.commerce.financial.settlement.domain.model.SettlementSummary(
            CAST(COUNT(s) AS Integer),
            COALESCE(SUM(s.grossAmount), 0),
            COALESCE(SUM(s.feeAmount), 0),
            COALESCE(SUM(s.netAmount), 0)
        )
        FROM Settlement s
        WHERE s.insertedAt >= :startDate
          AND s.insertedAt < :endDate
          AND s.walletId = :walletId
          AND s.state = :state
	  """)
	SettlementSummary summarizeByInsertedAtBetween(
		@Param("startDate")LocalDateTime startDate,
		@Param("endDate")LocalDateTime endDate,
		@Param("walletId")Long walletId,
		@Param("state") SettlementStatus state
	);
}
