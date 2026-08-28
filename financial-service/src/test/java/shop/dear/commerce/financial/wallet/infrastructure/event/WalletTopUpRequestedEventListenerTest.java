package shop.dear.commerce.financial.wallet.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import shop.dear.commerce.financial.payment.application.event.WalletTopUpRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.TopUpCommand;
import shop.dear.commerce.financial.wallet.application.event.WalletTopUpSucceededEvent;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WalletTopUpRequestedEventListenerTest {
    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private WalletTopUpRequestedEventListener listener;

    @Test
    void handle_topUpsWallet_andPublishesSuccessEvent() {
        // given
        final WalletTopUpRequestedEvent event =
                new WalletTopUpRequestedEvent(
                        100L,
                        1L,
                        new BigDecimal("10000.00")
                );

        // when
        listener.handle(event);

        // then
        verify(walletService).topUp(
                new TopUpCommand(
                        1L,
                        new BigDecimal("10000.00"),
                        100L
                )
        );

        verify(applicationEventPublisher).publishEvent(
                new WalletTopUpSucceededEvent(100L)
        );
    }
}
