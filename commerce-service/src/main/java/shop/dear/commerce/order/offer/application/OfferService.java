package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.event.order.offer.OfferAcceptedEvent;
import shop.dear.common.exception.BusinessException;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OfferService {

  private static final List<OfferStatus> ACTIVE_STATUSES = List.of(
      OfferStatus.PENDING,
      OfferStatus.ACCEPTED
  );

  private final OfferRepository offerRepository;
  private final OfferEventPublisher offerEventPublisher;

  @Transactional
  public void acceptOffer(final Long offerId) {
    final Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new BusinessException(OfferErrorCode.OFFER_NOT_FOUND));

    offer.accept();

    offerEventPublisher.publish(new OfferAcceptedEvent(
        offer.getId(),
        offer.getBuyerId(),
        offer.getSellerId(),
        offer.getProductId(),
        offer.getAmount(),
        offer.getStatus().toString()
    ));
  }

  public boolean existsActiveOfferByProductId(final Long productId) {
    return offerRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES);
  }
}
