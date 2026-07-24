package shop.dear.commerce.order.offer.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.common.event.order.FinishedOrderEvent;

@RequiredArgsConstructor
@Component
public class SpringOfferEventPublisher implements OfferEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final FinishedOrderEvent event) {
      applicationEventPublisher.publishEvent(event);
    }
}
