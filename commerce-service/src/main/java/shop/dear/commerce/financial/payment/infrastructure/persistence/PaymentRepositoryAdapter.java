package shop.dear.commerce.financial.payment.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;
import shop.dear.commerce.financial.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import shop.dear.common.event.order.OrderType;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Optional<Payment> findById(final Long id) {
        return paymentJpaRepository.findById(id);
    }

    @Override
    public Payment save(final Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByMerchantOrderId(
            final String merchantOrderId
    ) {
        return paymentJpaRepository.findByMerchantOrderId(merchantOrderId);
    }

    @Override
    public Optional<Payment> findByOrderIdAndOrderType(
            final Long orderId,
            final OrderType orderType
    ) {
        return paymentJpaRepository.findByOrderIdAndOrderType(
                orderId,
                orderType
        );
    }
}
