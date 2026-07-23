package shop.dear.commerce.product.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.application.port.ProductEventPublisher;
import shop.dear.common.event.product.ProductChangedEvent;

@RequiredArgsConstructor
@Component
public class SpringProductEventPublisher implements ProductEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final ProductChangedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
