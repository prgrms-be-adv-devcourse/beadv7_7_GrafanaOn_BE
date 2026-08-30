package shop.dear.commerce.order.offer.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.common.event.FinishedOrderEventPublisher;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.type.OrderType;
import shop.dear.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class OfferPaymentEventListener {

    private final OfferRepository offerRepository;
    private final OfferEventPublisher offerEventPublisher;
    private final FinishedOrderEventPublisher finishedOrderEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        if (!OrderType.OFFER.name().equals(event.orderType())) {
            return;
        }

        final Offer offer = findOffer(event.orderId());
        offer.markPaid();

        final FinishedOrderEvent finishedOrderEvent = new FinishedOrderEvent(
                offer.getId(),
                offer.getBuyerId(),
                offer.getSellerId(),
                offer.getProductId(),
                offer.getAmount(),
                OrderType.OFFER.name()
        );

        offerEventPublisher.publish(finishedOrderEvent);
        finishedOrderEventPublisher.publish(finishedOrderEvent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(final PaymentFailedEvent event) {
        if (!OrderType.OFFER.name().equals(event.orderType())) {
            return;
        }

        findOffer(event.orderId()).markPaymentFailed();
    }

    private Offer findOffer(final Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OfferErrorCode.OFFER_NOT_FOUND));
    }
}
