package shop.dear.commerce.financial.payment.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import shop.dear.common.type.OrderType;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SpringWalletDebitEventPublisherTest {

    @Test
    void publish_delegatesToApplicationEventPublisher() {
        // given
        final ApplicationEventPublisher applicationEventPublisher =
                mock(ApplicationEventPublisher.class);

        final SpringWalletDebitEventPublisher publisher =
                new SpringWalletDebitEventPublisher(applicationEventPublisher);

        final WalletDebitRequestedEvent event = new WalletDebitRequestedEvent(
                100L,
                1L,
                new BigDecimal("10000.00"),
                OrderType.PURCHASE.name()
        );

        // when
        publisher.publish(event);

        // then
        verify(applicationEventPublisher).publishEvent(event);
    }
}
