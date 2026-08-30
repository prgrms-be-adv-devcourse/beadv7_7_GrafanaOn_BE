package shop.dear.commerce.financial.payment.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.port.PaymentFailedEventPublisher;
import shop.dear.common.event.financial.PaymentFailedEvent;

@Component
@RequiredArgsConstructor
public class OutboxPaymentFailedEventPublisher
        implements PaymentFailedEventPublisher {

    private final PaymentOutboxAppender paymentOutboxAppender;

    @Override
    public void publish(final PaymentFailedEvent event) {
        paymentOutboxAppender.append(event);
    }
}