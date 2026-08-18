package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.PayCommand;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitFailedEvent;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitSucceededEvent;
import shop.dear.common.type.OrderType;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDebitRequestedEventListener {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final WalletService walletService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final WalletDebitRequestedEvent event) {
        final PayCommand command = new PayCommand(
                event.memberId(),
                event.amount(),
                event.paymentId()
        );

        try {
            debit(command, OrderType.valueOf(event.orderType()));

            applicationEventPublisher.publishEvent(
                    new WalletDebitSucceededEvent(event.paymentId())
            );
        } catch (final RuntimeException exception) {
            log.error(
                    "지갑 차감 실패. paymentId={}",
                    event.paymentId(),
                    exception
            );

            applicationEventPublisher.publishEvent(
                    new WalletDebitFailedEvent(event.paymentId())
            );
        }
    }

    private void debit(
            final PayCommand command,
            final OrderType orderType
    ) {
        for (int attempt = 1; attempt <= MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            try {
                switch (orderType) {
                    case PURCHASE -> walletService.payAvailable(command);
                    case OFFER -> walletService.payHeld(command);
                }
                return;
            } catch (final ObjectOptimisticLockingFailureException exception) {
                if (attempt == MAX_OPTIMISTIC_LOCK_ATTEMPTS) {
                    throw exception;
                }

                log.warn(
                        "지갑 낙관적 락 충돌 재시도. paymentId={}, attempt={}",
                        command.paymentId(),
                        attempt
                );
            }
        }
    }
}
