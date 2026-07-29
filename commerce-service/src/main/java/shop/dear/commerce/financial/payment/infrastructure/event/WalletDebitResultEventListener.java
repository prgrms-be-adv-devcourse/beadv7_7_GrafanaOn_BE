package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitFailedEvent;
import shop.dear.commerce.financial.wallet.application.event.WalletDebitSucceededEvent;

@Component
@RequiredArgsConstructor
public class WalletDebitResultEventListener {

    private final PaymentService paymentService;

    @EventListener
    public void handle(final WalletDebitSucceededEvent event) {
        paymentService.completePayment(event.paymentId());
    }

    @EventListener
    public void handle(final WalletDebitFailedEvent event) {
        paymentService.failPayment(event.paymentId());
    }
}
