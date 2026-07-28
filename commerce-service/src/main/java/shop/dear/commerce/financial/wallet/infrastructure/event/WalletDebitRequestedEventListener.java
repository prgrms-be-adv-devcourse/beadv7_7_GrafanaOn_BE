package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.PayCommand;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitFailedEvent;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitSucceededEvent;
import shop.dear.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class WalletDebitRequestedEventListener {

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
            switch (event.orderType()) {
                case PURCHASE -> walletService.payAvailable(command);
                case OFFER -> walletService.payHeld(command);
            }

            applicationEventPublisher.publishEvent(
                    new WalletDebitSucceededEvent(event.paymentId())
            );
        } catch (final BusinessException exception) {
            applicationEventPublisher.publishEvent(
                    new WalletDebitFailedEvent(event.paymentId())
            );
        }
    }
}
