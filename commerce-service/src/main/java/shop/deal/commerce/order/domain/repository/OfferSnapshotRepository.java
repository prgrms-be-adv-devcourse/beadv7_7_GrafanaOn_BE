package shop.deal.commerce.order.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.domain.OfferSnapshot;

public interface OfferSnapshotRepository extends JpaRepository<OfferSnapshot, Long> {
}
