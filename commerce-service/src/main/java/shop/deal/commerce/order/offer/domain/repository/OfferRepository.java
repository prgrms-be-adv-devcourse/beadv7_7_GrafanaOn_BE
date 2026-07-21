package shop.deal.commerce.order.offer.domain.repository;

import shop.deal.commerce.order.offer.domain.Offer;

import java.util.Optional;

public interface OfferRepository {

    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);
}
