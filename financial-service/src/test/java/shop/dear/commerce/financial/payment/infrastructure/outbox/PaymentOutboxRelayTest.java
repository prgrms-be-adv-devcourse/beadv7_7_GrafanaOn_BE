package shop.dear.commerce.financial.payment.infrastructure.outbox;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private PaymentOutboxRelayProcessor paymentOutboxRelayProcessor;

    @Mock
    private PaymentOutbox pendingOutbox;

    @Mock
    private PaymentOutbox failedOutbox;

    @InjectMocks
    private PaymentOutboxRelay paymentOutboxRelay;

    @Test
    void relaysPendingAndFailedOutboxesIndependently() {
        ReflectionTestUtils.setField(paymentOutboxRelay, "maxRetryCount", 3);
        given(pendingOutbox.getId()).willReturn(1L);
        given(failedOutbox.getId()).willReturn(2L);
        given(paymentOutboxRepository.findTop100ByStatusInOrderByInsertedAtAsc(
                List.of(PaymentOutboxStatus.PENDING, PaymentOutboxStatus.FAILED)
        )).willReturn(List.of(pendingOutbox, failedOutbox));

        paymentOutboxRelay.relay();

        verify(paymentOutboxRelayProcessor).process(1L, 3);
        verify(paymentOutboxRelayProcessor).process(2L, 3);
    }
}
