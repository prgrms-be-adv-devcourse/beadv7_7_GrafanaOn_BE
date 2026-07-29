package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.HoldCommand;

@Component
@RequiredArgsConstructor
public class PaymentHoldRequestedEventListener {

    private final WalletService walletService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final PaymentHoldRequestedEvent event) {
        if (event.orderType() != OrderType.OFFER) {
            return;
        }

        walletService.hold(new HoldCommand(
                event.memberId(),
                event.amount(),
                event.orderId()
        ));
    }
}
