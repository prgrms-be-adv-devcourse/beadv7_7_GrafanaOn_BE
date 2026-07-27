package shop.dear.commerce.order.offersnapshot.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;

public interface OfferSnapshotJpaRepository extends JpaRepository<OfferSnapshot, Long> {
}
