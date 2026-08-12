package shop.dear.commerce.order.purchase.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseJpaRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByBuyerId(Long buyerId);

    List<Purchase> findAllByStatusAndPaymentDueAtBefore(PurchaseStatus status, LocalDateTime paymentDueAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Purchase p SET p.status = :to, p.version = p.version + 1 "
            + "WHERE p.id = :id AND p.status = :from")
    int updateStatusIfCurrent(@Param("id") Long id, @Param("from") PurchaseStatus from, @Param("to") PurchaseStatus to);
}