package shop.dear.commerce.financial.payment.application.port;

import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;

public interface WalletDebitEventPublisher {

    void publish(WalletDebitRequestedEvent event);
}
