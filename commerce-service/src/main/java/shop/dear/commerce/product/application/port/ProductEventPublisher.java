package shop.dear.commerce.product.application.port;

import shop.dear.common.event.product.ProductChangedEvent;

public interface ProductEventPublisher {
    void publish(ProductChangedEvent event);
}
