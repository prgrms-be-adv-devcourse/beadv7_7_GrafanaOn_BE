package shop.deal.commerce.order.purchase.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.purchase.domain.model.Purchase;

public interface PurchaseJpaRepository extends JpaRepository<Purchase, Long> {
}