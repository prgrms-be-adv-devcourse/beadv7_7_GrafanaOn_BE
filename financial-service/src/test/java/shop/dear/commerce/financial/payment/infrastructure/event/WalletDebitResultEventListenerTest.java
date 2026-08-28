package shop.dear.commerce.financial.payment.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitFailedEvent;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitSucceededEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WalletDebitResultEventListenerTest {
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private WalletDebitResultEventListener listener;

    @Test
    void handleSucceededEvent_completesPayment() {
        listener.handle(new WalletDebitSucceededEvent(100L));

        verify(paymentService).completePayment(100L);
    }

    @Test
    void handleFailedEvent_failsPayment() {
        listener.handle(new WalletDebitFailedEvent(100L));

        verify(paymentService).failPayment(100L);
    }
}
