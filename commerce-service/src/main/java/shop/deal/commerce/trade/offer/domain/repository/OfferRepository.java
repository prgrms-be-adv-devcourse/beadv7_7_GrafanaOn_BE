package shop.deal.commerce.trade.offer.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.trade.offer.domain.Offer;

public interface OfferRepository extends JpaRepository<Offer, Long> {
}
