package shop.dear.commerce.financial.settlementpolicy.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;

import java.util.Optional;

public interface SettlementPolicyJpaRepository extends JpaRepository<SettlementPolicy, Long> {

    Optional<SettlementPolicy> findFirstByOrderByIdAsc();

}
