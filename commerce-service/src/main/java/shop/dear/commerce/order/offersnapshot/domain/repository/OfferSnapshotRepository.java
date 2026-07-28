package shop.dear.commerce.order.offersnapshot.domain.repository;

import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;

import java.util.Optional;

public interface OfferSnapshotRepository {

    OfferSnapshot save(OfferSnapshot snapshot);

    Optional<OfferSnapshot> findById(Long id);

    Optional<OfferSnapshot> findFirstByWriterIdAndProductId(Long writerId, Long productId);
}
