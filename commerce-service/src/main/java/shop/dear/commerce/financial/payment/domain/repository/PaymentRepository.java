package shop.dear.commerce.financial.payment.domain.repository;

import shop.dear.commerce.financial.payment.domain.model.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);

    Payment save(Payment payment);
}
