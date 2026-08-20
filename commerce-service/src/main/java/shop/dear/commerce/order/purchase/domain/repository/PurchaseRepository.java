package shop.dear.commerce.order.purchase.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Purchase> findByBuyerId(Long buyerId, Pageable pageable);

    List<Purchase> findAllByStatusAndPaymentDueAtBefore(PurchaseStatus status, LocalDateTime paymentDueAt);

    int updateStatusIfCurrent(Long id, PurchaseStatus from, PurchaseStatus to);
}
