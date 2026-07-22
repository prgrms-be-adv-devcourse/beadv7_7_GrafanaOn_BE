package shop.deal.commerce.order.purchase.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.order.purchase.domain.model.Purchase;
import shop.deal.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.deal.commerce.order.purchase.infrastructure.persistence.jpa.PurchaseJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PurchaseRepositoryAdapter implements PurchaseRepository {

  private final PurchaseJpaRepository purchaseJpaRepository;

  @Override
  public Purchase save(final Purchase purchase) {
    return purchaseJpaRepository.save(purchase);
  }

  @Override
  public Optional<Purchase> findById(final Long id) {
    return purchaseJpaRepository.findById(id);
  }

  @Override
  public boolean existsById(final Long id) {
    return purchaseJpaRepository.existsById(id);
  }

  @Override
  public void deleteById(final Long id) {
    purchaseJpaRepository.deleteById(id);
  }
}
