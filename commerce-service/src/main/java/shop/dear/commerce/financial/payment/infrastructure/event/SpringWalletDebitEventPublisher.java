package shop.dear.commerce.financial.payment.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.WalletDebitEventPublisher;

@Component
@RequiredArgsConstructor
public class SpringWalletDebitEventPublisher implements WalletDebitEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final WalletDebitRequestedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
