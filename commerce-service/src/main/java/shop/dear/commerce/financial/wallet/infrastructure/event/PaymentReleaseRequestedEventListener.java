package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.ReleaseCommand;

@Component
@RequiredArgsConstructor
public class PaymentReleaseRequestedEventListener {

    private final WalletService walletService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final PaymentReleaseRequestedEvent event) {
        walletService.release(new ReleaseCommand(
                event.memberId(),
                event.amount(),
                event.orderId()
        ));
    }
}
