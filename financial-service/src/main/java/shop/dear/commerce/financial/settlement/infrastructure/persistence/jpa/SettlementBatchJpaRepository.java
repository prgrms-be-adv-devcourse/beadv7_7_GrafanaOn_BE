package shop.dear.commerce.financial.settlement.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatch, Long> {

	Optional<SettlementBatch> findByPeriodAndWalletId(String period, Long walletId);

	List<SettlementBatch> findByPeriodAndState(String period, SettlementBatchStatus state);
}
