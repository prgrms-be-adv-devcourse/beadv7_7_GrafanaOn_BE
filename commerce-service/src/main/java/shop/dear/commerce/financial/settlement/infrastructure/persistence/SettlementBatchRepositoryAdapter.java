package shop.dear.commerce.financial.settlement.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.infrastructure.persistence.jpa.SettlementBatchJpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementBatchRepositoryAdapter implements SettlementBatchRepository {

    private final SettlementBatchJpaRepository settlementBatchJpaRepository;

    @Override
    public Optional<SettlementBatch> findByPeriodAndWalletId(final String period, final Long walletId) {
        return settlementBatchJpaRepository.findByPeriodAndWalletId(period, walletId);
    }

    @Override
    public List<SettlementBatch> findByPeriodAndState(final String period, final SettlementBatchStatus state) {
        return settlementBatchJpaRepository.findByPeriodAndState(period, state);
    }

    @Override
    public SettlementBatch save(final SettlementBatch settlementBatch) {
        return settlementBatchJpaRepository.save(settlementBatch);
    }

    @Override
    public List<SettlementBatch> saveAll(final List<SettlementBatch> settlementBatches) {
        return settlementBatchJpaRepository.saveAll(settlementBatches);
    }
}
