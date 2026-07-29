package shop.dear.commerce.financial.payment.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.common.event.order.OrderType;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    @Query("""
          select p from Payment p
          join fetch p.pgPayment pg
          where pg.merchantOrderId = :merchantOrderId
          """)
    Optional<Payment> findByMerchantOrderId(
            @Param("merchantOrderId")
            final String merchantOrderId
    );

    Optional<Payment> findByOrderIdAndOrderType(
            final Long orderId,
            final OrderType orderType
    );
}
