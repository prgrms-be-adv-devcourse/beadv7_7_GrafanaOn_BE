package shop.dear.commerce.financial.payment.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOutboxTest {

    @Test
    void createsPendingOutboxWithEventId() {
        final PaymentOutbox outbox = PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_COMPLETED,
                """
                {"paymentId":1}
                """
        );

        assertThat(outbox.getEventId()).isNotBlank();
        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
        assertThat(outbox.getPaymentId()).isEqualTo(1L);
        assertThat(outbox.getEventType())
                .isEqualTo(PaymentOutboxEventType.PAYMENT_COMPLETED);
    }

    @Test
    void marksOutboxAsFailedAndIncreasesRetryCount() {
        final PaymentOutbox outbox = PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_FAILED,
                """
                {"paymentId":1}
                """
        );

        outbox.markFailed("Order service timeout");

        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("Order service timeout");
    }

    @Test
    void marksOutboxAsSent() {
        final PaymentOutbox outbox = PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_COMPLETED,
                """
                {"paymentId":1}
                """
        );

        outbox.markSent();

        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isNotNull();
    }
}
