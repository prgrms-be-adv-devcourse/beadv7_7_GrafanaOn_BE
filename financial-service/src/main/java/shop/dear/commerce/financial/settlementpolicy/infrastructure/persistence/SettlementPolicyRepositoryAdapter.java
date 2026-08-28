package shop.dear.commerce.financial.settlementpolicy.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.commerce.financial.settlementpolicy.domain.repository.SettlementPolicyRepository;
import shop.dear.commerce.financial.settlementpolicy.infrastructure.persistence.jpa.SettlementPolicyJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementPolicyRepositoryAdapter implements SettlementPolicyRepository {

    private final SettlementPolicyJpaRepository settlementPolicyJpaRepository;

    @Override
    public Optional<SettlementPolicy> findFirstByOrderByIdAsc() {
        return settlementPolicyJpaRepository.findFirstByOrderByIdAsc();
    }

    @Override
    public SettlementPolicy save(final SettlementPolicy settlementPolicy) {
        return settlementPolicyJpaRepository.save(settlementPolicy);
    }

}
