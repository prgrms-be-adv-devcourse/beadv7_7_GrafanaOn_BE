package shop.dear.commerce.order.offer.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class OfferPaymentEventListener {

    private final OfferRepository offerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        if (event.orderType() != OrderType.OFFER) {
            return;
        }

        findOffer(event.orderId()).markPaid();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(final PaymentFailedEvent event) {
        if (event.orderType() != OrderType.OFFER) {
            return;
        }

        findOffer(event.orderId()).markPaymentFailed();
    }

    private Offer findOffer(final Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OfferErrorCode.OFFER_NOT_FOUND));
    }
}
