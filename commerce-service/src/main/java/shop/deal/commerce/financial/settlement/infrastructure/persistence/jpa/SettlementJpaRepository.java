package shop.deal.commerce.financial.settlement.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.deal.commerce.financial.settlement.domain.model.Settlement;

public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {
}
