package shop.deal.commerce.order.offer.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.order.offer.domain.model.Offer;
import shop.deal.commerce.order.offer.domain.repository.OfferRepository;
import shop.deal.commerce.order.offer.infrastructure.persistence.jpa.OfferJpaRepository;

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
}
