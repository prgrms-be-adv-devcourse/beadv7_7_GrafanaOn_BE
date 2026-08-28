package shop.dear.commerce.financial.settlement.domain.repository;

import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository {

	// 누적 대상 집계 조회 (없으면 새로 생성한다)
	Optional<SettlementBatch> findByPeriodAndWalletId(String period, Long walletId);

	// 마감 / 검증 / 지급 대상 조회
	List<SettlementBatch> findByPeriodAndState(String period, SettlementBatchStatus state);

	SettlementBatch save(SettlementBatch settlementBatch);

	List<SettlementBatch> saveAll(List<SettlementBatch> settlementBatches);
}
