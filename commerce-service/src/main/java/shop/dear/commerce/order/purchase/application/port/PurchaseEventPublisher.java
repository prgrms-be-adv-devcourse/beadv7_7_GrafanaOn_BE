package shop.dear.commerce.order.purchase.application.port;

import shop.dear.common.event.order.FinishedOrderEvent;

public interface PurchaseEventPublisher {

  void publish(FinishedOrderEvent event);

}
