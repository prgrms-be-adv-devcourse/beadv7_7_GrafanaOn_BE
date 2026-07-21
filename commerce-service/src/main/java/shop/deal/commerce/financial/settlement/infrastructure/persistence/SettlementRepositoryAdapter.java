package shop.deal.commerce.financial.settlement.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.deal.commerce.financial.settlement.infrastructure.persistence.jpa.SettlementJpaRepository;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SettlementJpaRepository settlementJpaRepository;

}




