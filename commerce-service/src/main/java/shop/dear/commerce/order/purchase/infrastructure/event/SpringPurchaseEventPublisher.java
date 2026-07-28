package shop.dear.commerce.order.purchase.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.common.event.order.FinishedOrderEvent;

@Component
@RequiredArgsConstructor
public class SpringPurchaseEventPublisher implements PurchaseEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void publish(FinishedOrderEvent event) {
    eventPublisher.publishEvent(event);
  }
}
