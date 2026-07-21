package shop.deal.commerce.financial.settlementpolicy.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.financial.settlementpolicy.domain.repository.SettlementPolicyRepository;
import shop.deal.commerce.financial.settlementpolicy.infrastructure.persistence.jpa.SettlementPolicyJpaRepository;

@Repository
@RequiredArgsConstructor
public class SettlementPolicyRepositoryAdapter implements SettlementPolicyRepository {

    private final SettlementPolicyJpaRepository settlementPolicyJpaRepository;

}
