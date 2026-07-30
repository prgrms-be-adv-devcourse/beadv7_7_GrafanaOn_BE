package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.common.event.financial.PaymentRequestedEvent;

@Component
@RequiredArgsConstructor
public class PaymentRequestedEventListener {

    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final PaymentRequestedEvent event) {
        paymentService.payOrder(new PayOrderCommand(
                event.memberId(),
                event.orderId(),
                event.orderType(),
                event.amount()
        ));
    }
}
