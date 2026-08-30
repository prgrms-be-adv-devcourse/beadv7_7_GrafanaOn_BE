package shop.dear.commerce.financial.payment.infrastructure.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.messaging.serializer.JsonPayloadSerializer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxAppenderTest {

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private JsonPayloadSerializer jsonPayloadSerializer;

    @InjectMocks
    private PaymentOutboxAppender paymentOutboxAppender;

    @Test
    void appendsCompletedPaymentEventAsPendingOutbox() {
        final PaymentCompletedEvent event = new PaymentCompletedEvent(
                1L,
                100L,
                "PURCHASE",
                10L,
                new BigDecimal("10000.00"),
                OffsetDateTime.now()
        );

        given(jsonPayloadSerializer.serialize(event))
                .willReturn("{\"paymentId\":1}");

        paymentOutboxAppender.append(event);

        final ArgumentCaptor<PaymentOutbox> captor =
                ArgumentCaptor.forClass(PaymentOutbox.class);

        verify(paymentOutboxRepository).save(captor.capture());

        final PaymentOutbox outbox = captor.getValue();
        assertThat(outbox.getPaymentId()).isEqualTo(1L);
        assertThat(outbox.getEventType())
                .isEqualTo(PaymentOutboxEventType.PAYMENT_COMPLETED);
        assertThat(outbox.getPayload()).isEqualTo("{\"paymentId\":1}");
        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
    }

    @Test
    void appendsFailedPaymentEventAsPendingOutbox() {
        final PaymentFailedEvent event = new PaymentFailedEvent(
                1L,
                100L,
                "PURCHASE",
                10L,
                new BigDecimal("10000.00")
        );

        given(jsonPayloadSerializer.serialize(event))
                .willReturn("{\"paymentId\":1}");

        paymentOutboxAppender.append(event);

        final ArgumentCaptor<PaymentOutbox> captor =
                ArgumentCaptor.forClass(PaymentOutbox.class);

        verify(paymentOutboxRepository).save(captor.capture());

        final PaymentOutbox outbox = captor.getValue();
        assertThat(outbox.getPaymentId()).isEqualTo(1L);
        assertThat(outbox.getEventType())
                .isEqualTo(PaymentOutboxEventType.PAYMENT_FAILED);
        assertThat(outbox.getPayload()).isEqualTo("{\"paymentId\":1}");
        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
    }
}