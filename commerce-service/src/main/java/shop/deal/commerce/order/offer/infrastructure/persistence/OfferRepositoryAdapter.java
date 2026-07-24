package shop.dear.commerce.order.offer.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.commerce.order.offer.infrastructure.persistence.jpa.OfferJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OfferRepositoryAdapter implements OfferRepository {

  private final OfferJpaRepository offerJpaRepository;

  @Override
  public Offer save(final Offer offer) {
    return offerJpaRepository.save(offer);
  }

  @Override
  public Optional<Offer> findById(final Long id) {
    return offerJpaRepository.findById(id);
  }

  @Override
  public boolean existsById(final Long id) {
    return offerJpaRepository.existsById(id);
  }

  @Override
  public void deleteById(final Long id) {
    offerJpaRepository.deleteById(id);
  }

  @Override
  public boolean existsByProductIdAndStatusIn(final Long productId, final List<OfferStatus> statuses) {
    return offerJpaRepository.existsByProductIdAndStatusIn(productId, statuses);
  }
}
