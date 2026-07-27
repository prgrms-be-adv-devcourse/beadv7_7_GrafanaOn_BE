package shop.dear.commerce.product.application.port;

import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;

public interface ProductEventPublisher {
    void publish(final ProductChangedEvent event);
    void publish(final ProductDeletedEvent event);
}
