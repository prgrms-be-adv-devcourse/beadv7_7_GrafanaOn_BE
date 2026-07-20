package shop.deal.commerce.order.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.domain.Offer;

public interface OfferRepository extends JpaRepository<Offer, Long> {
}
