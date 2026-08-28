package shop.dear.commerce.financial.wallet.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.ReleaseCommand;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentReleaseRequestedEventListenerTest {

    private static final Long OFFER_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");
    private static final String REASON = "OFFER_REJECTED";

    @Mock
    private WalletService walletService;

    @InjectMocks
    private PaymentReleaseRequestedEventListener listener;

    @Test
    void handleOffer_releasesBuyerBalance() {
        final PaymentReleaseRequestedEvent event =
                new PaymentReleaseRequestedEvent(
                        OFFER_ID,
                        MEMBER_ID,
                        AMOUNT,
                        REASON
                );

        listener.handle(event);

        verify(walletService).release(new ReleaseCommand(
                MEMBER_ID,
                AMOUNT,
                OFFER_ID
        ));
    }
}