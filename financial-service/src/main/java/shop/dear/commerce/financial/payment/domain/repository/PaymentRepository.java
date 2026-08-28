package shop.dear.commerce.financial.payment.domain.repository;

import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.common.type.OrderType;

import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);

    Payment save(Payment payment);

    Optional<Payment> findByMerchantOrderId(String merchantOrderId);

    Optional<Payment> findByOrderIdAndOrderType(Long orderId, OrderType orderType);
}
