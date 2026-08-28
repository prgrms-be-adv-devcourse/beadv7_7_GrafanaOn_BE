package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.common.event.financial.PaymentRequestedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedEventListener {

    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final PaymentRequestedEvent event) {
        try {
            paymentService.payOrder(new PayOrderCommand(
                    event.memberId(),
                    event.orderId(),
                    event.orderType(),
                    event.amount()
            ));
        } catch (final IllegalArgumentException exception) {
            log.error(
                    "지원하지 않는 주문 타입. orderId={}, orderType={}",
                    event.orderId(),
                    event.orderType(),
                    exception
            );
        }
    }
}
