package shop.dear.commerce.financial.payment.application.port;

import shop.dear.common.event.financial.PaymentFailedEvent;

public interface PaymentFailedEventPublisher {

    void publish(PaymentFailedEvent event);

}
