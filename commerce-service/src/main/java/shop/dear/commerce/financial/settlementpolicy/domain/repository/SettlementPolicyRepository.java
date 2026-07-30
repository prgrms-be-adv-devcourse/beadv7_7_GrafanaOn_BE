package shop.dear.commerce.financial.settlementpolicy.domain.repository;

import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;

import java.util.Optional;

public interface SettlementPolicyRepository {

    Optional<SettlementPolicy> findFirstByOrderByIdAsc();

    SettlementPolicy save(SettlementPolicy settlementPolicy);
}
