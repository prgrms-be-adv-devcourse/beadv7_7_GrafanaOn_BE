package shop.dear.commerce.financial.wallet.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.infrastructure.event.WalletTopUpResultEventListener;
import shop.dear.commerce.financial.wallet.application.event.WalletTopUpSucceededEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WalletTopUpResultEventListenerTest {
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private WalletTopUpResultEventListener listener;

    @Test
    void handle_completesPayment() {
        // when
        listener.handle(new WalletTopUpSucceededEvent(100L));

        // then
        verify(paymentService).completePayment(100L);
    }
}
