package shop.dear.commerce.order.offer.application.port;

import shop.dear.common.event.order.offer.OfferAcceptedEvent;

public interface OfferEventPublisher {
  void publish(OfferAcceptedEvent event);
}
