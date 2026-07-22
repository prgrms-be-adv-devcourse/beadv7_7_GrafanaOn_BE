package shop.deal.commerce.order.offer.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.offer.domain.model.Offer;

public interface OfferJpaRepository extends JpaRepository<Offer, Long> {
}