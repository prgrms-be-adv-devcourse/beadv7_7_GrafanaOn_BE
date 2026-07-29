package shop.dear.commerce.financial.payment.application.port;

import shop.dear.commerce.financial.payment.application.event.WalletTopUpRequestedEvent;

public interface WalletTopUpEventPublisher {

    void publish(WalletTopUpRequestedEvent event);

}
