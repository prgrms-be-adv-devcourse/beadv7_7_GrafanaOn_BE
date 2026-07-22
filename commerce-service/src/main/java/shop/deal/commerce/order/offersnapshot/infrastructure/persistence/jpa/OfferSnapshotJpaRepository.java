package shop.deal.commerce.order.offersnapshot.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.offersnapshot.domain.model.OfferSnapshot;

public interface OfferSnapshotJpaRepository extends JpaRepository<OfferSnapshot, Long> {
}
