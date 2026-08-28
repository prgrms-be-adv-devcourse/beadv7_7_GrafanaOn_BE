package shop.dear.commerce.financial.settlement.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementSummary;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.commerce.financial.settlement.infrastructure.persistence.jpa.SettlementJpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SettlementJpaRepository settlementJpaRepository;

    @Override
    public List<Settlement> findByInsertedAtBetween(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long walletId,
        SettlementStatus status
    ) {
        return settlementJpaRepository.findByInsertedAtBetween(
            startDate,
            endDate,
            walletId,
            status
        );
    }

    @Override
    public List<Settlement> findByInsertedAtBefore(
        LocalDateTime baseDate,
        SettlementStatus status
    ) {
        return settlementJpaRepository.findByInsertedAtBefore(
            baseDate,
            status
        );
    }

    @Override
    public SettlementSummary summarizeByInsertedAtBetween(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long walletId,
        SettlementStatus status
    ) {
        return settlementJpaRepository.summarizeByInsertedAtBetween(
            startDate,
            endDate,
            walletId,
            status
        );
    }

    @Override
    public boolean existsByPurchaseId(final Long purchaseId) {
        return settlementJpaRepository.existsByPurchaseId(purchaseId);
    }

    @Override
    public boolean existsByOfferId(final Long offerId) {
        return settlementJpaRepository.existsByOfferId(offerId);
    }

    @Override
    public Settlement save(final Settlement settlement) {
        return settlementJpaRepository.save(settlement);
    }

    @Override
    public List<Settlement> saveAll(final List<Settlement> settlements) {
        return settlementJpaRepository.saveAll(settlements);
    }
}
