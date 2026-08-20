package shop.dear.commerce.order.purchase.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.commerce.order.purchase.infrastructure.persistence.jpa.PurchaseJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
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

  @Override
  public Page<Purchase> findByBuyerId(final Long buyerId, final Pageable pageable) {
    return purchaseJpaRepository.findByBuyerId(buyerId, pageable);
  }

  @Override
  public List<Purchase> findAllByStatusAndPaymentDueAtBefore(final PurchaseStatus status, final LocalDateTime paymentDueAt) {
    return purchaseJpaRepository.findAllByStatusAndPaymentDueAtBefore(status, paymentDueAt);
  }

  @Override
  public int updateStatusIfCurrent(final Long id, final PurchaseStatus from, final PurchaseStatus to) {
    return purchaseJpaRepository.updateStatusIfCurrent(id, from, to);
  }
}
