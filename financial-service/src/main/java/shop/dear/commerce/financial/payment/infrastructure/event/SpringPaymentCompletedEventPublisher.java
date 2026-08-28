package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.port.PaymentCompletedEventPublisher;
import shop.dear.common.event.financial.PaymentCompletedEvent;

@Component
@RequiredArgsConstructor
public class SpringPaymentCompletedEventPublisher implements PaymentCompletedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final PaymentCompletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
