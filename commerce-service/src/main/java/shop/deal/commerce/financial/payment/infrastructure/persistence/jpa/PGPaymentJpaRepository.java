package shop.deal.commerce.financial.payment.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.deal.commerce.financial.payment.domain.model.PGPayment;

public interface PGPaymentJpaRepository extends JpaRepository<PGPayment, Long> {
}
