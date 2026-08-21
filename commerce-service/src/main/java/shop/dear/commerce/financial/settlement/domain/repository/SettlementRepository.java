package shop.dear.commerce.financial.settlement.domain.repository;

import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;

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

	// 특정 기간 동안 정산 대상인 지갑 목록
	List<Long> findWalletIdsByInsertedAtBetween(
		LocalDateTime startDate,
		LocalDateTime endDate,
		SettlementStatus status
	);


	Settlement save(final Settlement settlement);

	List<Settlement> saveAll(List<Settlement> settlements);

	boolean existsByPurchaseIdOrOfferId(Long purchaseId, Long offerId);
}
