package shop.dear.commerce.product.application.port;

import shop.dear.commerce.product.domain.event.ProductChangedEvent;

public interface ProductEventPublisher {
    void publish(ProductChangedEvent event);
}
