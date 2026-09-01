package shop.dear.commerce.financial.payment.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.payment.infrastructure.client.OrderPaymentResultClient;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayProcessorTest {

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private OrderPaymentResultClient orderPaymentResultClient;

    @InjectMocks
    private PaymentOutboxRelayProcessor paymentOutboxRelayProcessor;

    @Test
    void marksOutboxAsSentWhenOrderDeliverySucceeds() {
        final PaymentOutbox outbox = paymentOutbox();
        given(paymentOutboxRepository.findById(1L))
                .willReturn(Optional.of(outbox));

        paymentOutboxRelayProcessor.process(1L, 3);

        verify(orderPaymentResultClient).send(outbox);
        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isNotNull();
    }

    @Test
    void marksOutboxAsFailedBeforeReachingMaxRetryCount() {
        final PaymentOutbox outbox = paymentOutbox();
        given(paymentOutboxRepository.findById(1L))
                .willReturn(Optional.of(outbox));
        willThrow(new IllegalStateException("Order service timeout"))
                .given(orderPaymentResultClient)
                .send(outbox);

        paymentOutboxRelayProcessor.process(1L, 3);

        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("Order service timeout");
    }

    @Test
    void marksOutboxAsExhaustedWhenReachingMaxRetryCount() {
        final PaymentOutbox outbox = paymentOutbox();
        given(paymentOutboxRepository.findById(1L))
                .willReturn(Optional.of(outbox));
        willThrow(new IllegalStateException("Order service timeout"))
                .given(orderPaymentResultClient)
                .send(outbox);

        // 첫 실패 후 retryCount가 1이 되어, 최대 재시도 횟수에 도달한다.
        paymentOutboxRelayProcessor.process(1L, 1);

        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.EXHAUSTED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
    }

    private PaymentOutbox paymentOutbox() {
        return PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_COMPLETED,
                "{\"paymentId\":1}"
        );
    }
}
