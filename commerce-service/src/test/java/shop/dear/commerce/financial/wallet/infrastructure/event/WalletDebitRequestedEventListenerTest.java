package shop.dear.commerce.financial.wallet.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.PayCommand;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitFailedEvent;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitSucceededEvent;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletDebitRequestedEventListenerTest {
    private static final Long PAYMENT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");

    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private WalletDebitRequestedEventListener listener;

    @Test
    void handlePurchase_callsPayAvailable_andPublishesSuccess() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.PURCHASE
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        // when
        listener.handle(event);

        // then
        verify(walletService).payAvailable(command);
        verify(walletService, never()).payHeld(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
    }

    @Test
    void handleOffer_callsPayHeld_andPublishesSuccess() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.OFFER
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        // when
        listener.handle(event);

        // then
        verify(walletService).payHeld(command);
        verify(walletService, never()).payAvailable(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
    }

    @Test
    void handlePurchase_whenWalletBusinessException_publishesFailure() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.PURCHASE
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        doThrow(new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE))
                .when(walletService)
                .payAvailable(command);

        // when
        listener.handle(event);

        // then
        verify(walletService).payAvailable(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
    }

    @Test
    void handleOffer_whenWalletBusinessException_publishesFailure() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.OFFER
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        doThrow(new BusinessException(WalletErrorCode.INSUFFICIENT_HELD_BALANCE))
                .when(walletService)
                .payHeld(command);

        // when
        listener.handle(event);

        // then
        verify(walletService).payHeld(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
    }

    @Test
    void handlePurchase_whenOptimisticLockFailsThenSucceeds_retriesAndPublishesSuccess() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.PURCHASE
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        doThrow(new ObjectOptimisticLockingFailureException(
                "Wallet",
                MEMBER_ID
        )).doNothing()
                .when(walletService)
                .payAvailable(command);

        // when
        listener.handle(event);

        // then
        verify(walletService, times(2)).payAvailable(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
    }

    @Test
    void handlePurchase_whenOptimisticLockPersists_publishesFailureAfterThreeAttempts() {
        // given
        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                PAYMENT_ID,
                MEMBER_ID,
                AMOUNT,
                OrderType.PURCHASE
        );
        final PayCommand command = new PayCommand(
                MEMBER_ID,
                AMOUNT,
                PAYMENT_ID
        );

        doThrow(new ObjectOptimisticLockingFailureException(
                "Wallet",
                MEMBER_ID
        )).when(walletService)
                .payAvailable(command);

        // when
        listener.handle(event);

        // then
        verify(walletService, times(3)).payAvailable(command);
        verify(applicationEventPublisher).publishEvent(
                new WalletDebitFailedEvent(PAYMENT_ID)
        );
        verify(applicationEventPublisher, never()).publishEvent(
                new WalletDebitSucceededEvent(PAYMENT_ID)
        );
    }
}
