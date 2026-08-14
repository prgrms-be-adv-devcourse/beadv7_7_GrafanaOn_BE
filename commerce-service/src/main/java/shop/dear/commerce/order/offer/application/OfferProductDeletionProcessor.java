package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class OfferProductDeletionProcessor {

    private final OfferRepository offerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProductDeleted(final Long offerId) {
        final Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OfferErrorCode.OFFER_NOT_FOUND));

        offer.markProductDeleted();
    }
}
