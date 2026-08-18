package shop.dear.commerce.financial.settlement.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.type.OrderType;
import shop.dear.commerce.financial.settlement.application.SettlementService;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SettlementOrderEventListenerTest {

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private SettlementOrderEventListener listener;

    @Test
    void handle_createsPendingSettlementWhenOrderIsFinished() {
        // given
        final FinishedOrderEvent event = new FinishedOrderEvent(
                1L,
                2L,
                3L,
                4L,
                new BigDecimal("10000.00"),
                OrderType.PURCHASE.name()
        );

        // when
        listener.handle(event);

        // then
        verify(settlementService).createPendingSettlement(event);
    }
}
