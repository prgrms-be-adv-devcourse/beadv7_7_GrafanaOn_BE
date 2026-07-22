package shop.dear.commerce.order.offer.domain.repository;

import shop.dear.commerce.order.offer.domain.model.Offer;

import java.util.Optional;

public interface OfferRepository {

    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);
}
