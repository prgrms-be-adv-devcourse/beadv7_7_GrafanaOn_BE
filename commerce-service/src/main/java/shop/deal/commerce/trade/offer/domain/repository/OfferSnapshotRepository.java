package shop.deal.commerce.trade.offer.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.trade.offer.domain.OfferSnapshot;

public interface OfferSnapshotRepository extends JpaRepository<OfferSnapshot, Long> {
}
