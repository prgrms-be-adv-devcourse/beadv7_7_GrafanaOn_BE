package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.port.PaymentFailedEventPublisher;
import shop.dear.common.event.financial.PaymentFailedEvent;

@Component
@RequiredArgsConstructor
public class SpringPaymentFailedEventPublisher implements PaymentFailedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final PaymentFailedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
