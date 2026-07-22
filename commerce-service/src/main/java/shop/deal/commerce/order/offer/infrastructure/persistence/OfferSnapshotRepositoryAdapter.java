package shop.deal.commerce.order.offer.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.order.offer.domain.repository.OfferSnapshotRepository;
import shop.deal.commerce.order.offer.infrastructure.persistence.jpa.OfferSnapshotJpaRepository;

@Repository
@RequiredArgsConstructor
public class OfferSnapshotRepositoryAdapter implements OfferSnapshotRepository {

  private final OfferSnapshotJpaRepository offerSnapshotJpaRepository;
}
