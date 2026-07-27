package shop.dear.commerce.product.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.application.port.ProductEventPublisher;
import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;

@Slf4j
@RequiredArgsConstructor
@Component
public class SpringProductEventPublisher implements ProductEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final ProductChangedEvent event) {
        log.info("[SpringProductEventPublisher] Published ProductChangedEvent: {}", event);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(final ProductDeletedEvent event) {
        log.info("[SpringProductEventPublisher] Published ProductDeletedEvent: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
}
