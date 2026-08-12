package shop.dear.commerce.order.purchase.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.CanceledPurchaseEvent;
import shop.dear.common.event.order.FinishedOrderEvent;

@Component
@RequiredArgsConstructor
public class SpringPurchaseEventPublisher implements PurchaseEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void publish(FinishedOrderEvent event) {
    eventPublisher.publishEvent(event);
  }

  @Override
  public void publish(PaymentRequestedEvent event) {
    eventPublisher.publishEvent(event);
  }

  @Override
  public void publish(CanceledPurchaseEvent event) {
    eventPublisher.publishEvent(event);
  }
}
