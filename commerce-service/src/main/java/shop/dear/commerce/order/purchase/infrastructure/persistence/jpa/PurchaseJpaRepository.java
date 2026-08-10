package shop.dear.commerce.order.purchase.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseJpaRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByBuyerId(Long buyerId);

    List<Purchase> findAllByStatusAndPaymentDueAtBefore(PurchaseStatus status, LocalDateTime paymentDueAt);
}