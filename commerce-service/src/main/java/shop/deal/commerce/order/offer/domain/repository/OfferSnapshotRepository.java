package shop.deal.commerce.order.offer.domain.repository;

import shop.deal.commerce.order.offer.domain.OfferSnapshot;

import java.util.Optional;

public interface OfferSnapshotRepository {

    OfferSnapshot save(OfferSnapshot offerSnapshot);

    Optional<OfferSnapshot> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);
}
