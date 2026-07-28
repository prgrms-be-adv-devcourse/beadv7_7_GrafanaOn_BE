package shop.dear.commerce.order.offersnapshot.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.commerce.order.offersnapshot.domain.repository.OfferSnapshotRepository;
import shop.dear.commerce.order.offersnapshot.infrastructure.persistence.jpa.OfferSnapshotJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OfferSnapshotRepositoryAdapter implements OfferSnapshotRepository {

  private final OfferSnapshotJpaRepository offerSnapshotJpaRepository;

  @Override
  public OfferSnapshot save(final OfferSnapshot snapshot) {
    return offerSnapshotJpaRepository.save(snapshot);
  }

  @Override
  public Optional<OfferSnapshot> findById(final Long id) {
    return offerSnapshotJpaRepository.findById(id);
  }

  @Override
  public Optional<OfferSnapshot> findFirstByWriterIdAndProductId(final Long writerId, final Long productId) {
    return offerSnapshotJpaRepository.findFirstByWriterIdAndProductId(writerId, productId);
  }
}
