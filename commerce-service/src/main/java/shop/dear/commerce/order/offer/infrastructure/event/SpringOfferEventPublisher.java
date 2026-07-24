// infrastructure/event/SpringOfferEventPublisher.java
package shop.dear.commerce.order.offer.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.common.event.order.offer.OfferAcceptedEvent;

@RequiredArgsConstructor
@Component
public class SpringOfferEventPublisher implements OfferEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void publish(final OfferAcceptedEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
