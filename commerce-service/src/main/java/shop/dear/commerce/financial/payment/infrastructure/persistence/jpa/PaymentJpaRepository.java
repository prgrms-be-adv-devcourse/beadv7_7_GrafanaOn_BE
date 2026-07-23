package shop.dear.commerce.financial.payment.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.dear.commerce.financial.payment.domain.model.Payment;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
}
