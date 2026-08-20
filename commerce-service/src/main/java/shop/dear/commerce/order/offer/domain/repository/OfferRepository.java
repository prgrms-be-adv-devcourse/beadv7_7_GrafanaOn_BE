package shop.dear.commerce.order.offer.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Offer> findByProductIdAndStatusInOrderByInsertedAtDesc(Long productId, List<OfferStatus> statuses, Pageable pageable);

    Page<Offer> findByProductIdOrderByInsertedAtDesc(Long productId, Pageable pageable);

    List<Offer> findByProductIdAndStatusInOrderByInsertedAtDesc(Long productId, List<OfferStatus> statuses);
}
