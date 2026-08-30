package shop.dear.commerce.order.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.order.infrastructure.client.FinancialHttpClient;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.common.event.financial.PaymentRequestedEvent;

@Component
@RequiredArgsConstructor
public class FinancialEventForwarder {

    private final FinancialHttpClient financialHttpClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRequested(final PaymentRequestedEvent event) {
        financialHttpClient.requestPayment(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentHoldRequested(final PaymentHoldRequestedEvent event) {
        financialHttpClient.hold(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentReleaseRequested(final PaymentReleaseRequestedEvent event) {
        financialHttpClient.release(event);
    }
}
