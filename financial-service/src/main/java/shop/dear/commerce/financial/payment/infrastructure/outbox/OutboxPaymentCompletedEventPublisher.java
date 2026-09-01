package shop.dear.commerce.financial.payment.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.port.PaymentCompletedEventPublisher;
import shop.dear.common.event.financial.PaymentCompletedEvent;

@Component
@RequiredArgsConstructor
public class OutboxPaymentCompletedEventPublisher
        implements PaymentCompletedEventPublisher {

    private final PaymentOutboxAppender paymentOutboxAppender;

    @Override
    public void publish(final PaymentCompletedEvent event) {
        paymentOutboxAppender.append(event);
    }
}