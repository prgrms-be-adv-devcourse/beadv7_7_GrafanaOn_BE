package shop.deal.commerce.financial.payment.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.financial.payment.domain.repository.PaymentRepository;
import shop.deal.commerce.financial.payment.infrastructure.persistence.jpa.PaymentJpaRepository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

}
