package shop.deal.commerce.order.offersnapshot.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.order.offersnapshot.domain.repository.OfferSnapshotRepository;
import shop.deal.commerce.order.offersnapshot.infrastructure.persistence.jpa.OfferSnapshotJpaRepository;

@Repository
@RequiredArgsConstructor
public class OfferSnapshotRepositoryAdapter implements OfferSnapshotRepository {

  private final OfferSnapshotJpaRepository offerSnapshotJpaRepository;
}
