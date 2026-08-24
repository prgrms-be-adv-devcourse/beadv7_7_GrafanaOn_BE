package shop.dear.commerce.financial.settlement.domain.repository;

import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementSummary;

import java.time.LocalDateTime;
import java.util.List;

public interface SettlementRepository {
	// 특정 기간 동안의 정산데이터
	List<Settlement> findByInsertedAtBetween(
		LocalDateTime startDate,
		LocalDateTime endDate,
		Long walletId,
		SettlementStatus status
	);

	// 특정 기간, 특정 상태의 정산 건 전체 (지갑 무관)
	List<Settlement> findAllByInsertedAtBetween(
		LocalDateTime startDate,
		LocalDateTime endDate,
		SettlementStatus status
	);

	// 특정 지갑, 특정 기간의 정산 건수와 금액 합계
	SettlementSummary summarizeByInsertedAtBetween(
		LocalDateTime startDate,
		LocalDateTime endDate,
		Long walletId,
		SettlementStatus status
	);

	boolean existsByPurchaseId(Long purchaseId);

	boolean existsByOfferId(Long offerId);

	Settlement save(final Settlement settlement);

	List<Settlement> saveAll(List<Settlement> settlements);
}
