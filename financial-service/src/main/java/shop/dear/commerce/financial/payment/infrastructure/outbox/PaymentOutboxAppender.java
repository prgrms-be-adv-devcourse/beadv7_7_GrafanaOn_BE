package shop.dear.commerce.financial.payment.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.messaging.serializer.JsonPayloadSerializer;

@Component
@RequiredArgsConstructor
public class PaymentOutboxAppender {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final JsonPayloadSerializer jsonPayloadSerializer;

    public void append(final PaymentCompletedEvent event) {
        save(
                event.paymentId(),
                PaymentOutboxEventType.PAYMENT_COMPLETED,
                jsonPayloadSerializer.serialize(event)
        );
    }

    public void append(final PaymentFailedEvent event) {
        save(
                event.paymentId(),
                PaymentOutboxEventType.PAYMENT_FAILED,
                jsonPayloadSerializer.serialize(event)
        );
    }

    private void save(
            final Long paymentId,
            final PaymentOutboxEventType eventType,
            final String payload
    ) {
        paymentOutboxRepository.save(
                PaymentOutbox.of(paymentId, eventType, payload)
        );
    }
}