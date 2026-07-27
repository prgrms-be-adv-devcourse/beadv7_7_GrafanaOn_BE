package shop.dear.commerce.order.offer.domain.repository;

import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;

import java.util.List;
import java.util.Optional;

public interface OfferRepository {

    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);

    boolean existsByProductIdAndStatusIn(Long productId, List<OfferStatus> statuses);
}
