package shop.dear.commerce.order.purchase.domain.repository;

import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);

    List<Purchase> findByBuyerId(Long buyerId);

    List<Purchase> findAllByStatusAndPaymentDueAtBefore(PurchaseStatus status, LocalDateTime paymentDueAt);
}
