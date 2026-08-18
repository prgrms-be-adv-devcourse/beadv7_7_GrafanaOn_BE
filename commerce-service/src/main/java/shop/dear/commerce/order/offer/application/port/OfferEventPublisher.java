package shop.dear.commerce.order.offer.application.port;

import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.FinishedOrderEvent;

public interface OfferEventPublisher {
    void publish(FinishedOrderEvent event);

    void publish(PaymentHoldRequestedEvent event);

    void publish(PaymentRequestedEvent event);

    void publish(PaymentReleaseRequestedEvent event);
}
