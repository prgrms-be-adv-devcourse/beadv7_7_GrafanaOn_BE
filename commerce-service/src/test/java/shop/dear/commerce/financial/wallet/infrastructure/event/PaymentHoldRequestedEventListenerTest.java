package shop.dear.commerce.financial.wallet.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.HoldCommand;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PaymentHoldRequestedEventListenerTest {
    private static final Long OFFER_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");

    @Mock
    private WalletService walletService;

    @InjectMocks
    private PaymentHoldRequestedEventListener listener;

    @Test
    void handleOffer_holdsBuyerBalance() {
        PaymentHoldRequestedEvent event = new PaymentHoldRequestedEvent(
                OFFER_ID, MEMBER_ID, AMOUNT
        );

        listener.handle(event);

        verify(walletService).hold(new HoldCommand(
                MEMBER_ID, AMOUNT, OFFER_ID
        ));
    }
}
