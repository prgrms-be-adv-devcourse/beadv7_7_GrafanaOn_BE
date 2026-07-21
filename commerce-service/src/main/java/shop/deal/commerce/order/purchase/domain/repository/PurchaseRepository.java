package shop.deal.commerce.order.purchase.domain.repository;

import shop.deal.commerce.order.purchase.domain.Purchase;

import java.util.Optional;

public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);
}
