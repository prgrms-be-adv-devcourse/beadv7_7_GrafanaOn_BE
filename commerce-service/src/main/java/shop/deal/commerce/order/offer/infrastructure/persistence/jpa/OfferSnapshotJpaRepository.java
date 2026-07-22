package shop.deal.commerce.order.offer.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.offer.domain.model.OfferSnapshot;

public interface OfferSnapshotJpaRepository extends JpaRepository<OfferSnapshot, Long> {
}
