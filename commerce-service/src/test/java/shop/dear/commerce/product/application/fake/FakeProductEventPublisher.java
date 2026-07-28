package shop.dear.commerce.product.application.fake;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import shop.dear.commerce.product.application.port.ProductEventPublisher;
import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class FakeProductEventPublisher implements ProductEventPublisher {

    private final List<Object> events = new ArrayList<>();

    @Override
    public void publish(final ProductChangedEvent event) {
        log.info("[FakeProductEventPublisher] Published Event: {}", event);
        events.add(event);
    }

    @Override
    public void publish(final ProductDeletedEvent event) {
        log.info("[FakeProductEventPublisher] Published Event: {}", event);
        events.add(event);
    }
}
