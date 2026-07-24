package shop.dear.commerce.order.offer.application.port;

import shop.dear.common.event.order.FinishedOrderEvent;

public interface OfferEventPublisher {
    void publish(FinishedOrderEvent event);
}
