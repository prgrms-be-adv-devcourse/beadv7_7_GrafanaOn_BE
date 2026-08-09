package shop.dear.commerce.order.offer.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;

import java.util.List;

public interface OfferJpaRepository extends JpaRepository<Offer, Long> {

  boolean existsByProductIdAndStatusIn(Long productId, List<OfferStatus> statuses);

  List<Offer> findByProductIdAndStatusInOrderByInsertedAtDesc(Long productId, List<OfferStatus> statuses);

  List<Offer> findByProductIdOrderByInsertedAtDesc(Long productId);
}
