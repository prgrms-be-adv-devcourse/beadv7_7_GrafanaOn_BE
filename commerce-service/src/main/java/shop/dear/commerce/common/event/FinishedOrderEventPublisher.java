package shop.dear.commerce.common.event;

import shop.dear.common.event.order.FinishedOrderEvent;

public interface FinishedOrderEventPublisher {

    void publish(FinishedOrderEvent event);
}
