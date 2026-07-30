package shop.dear.commerce.financial.payment.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PaymentRequestedEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentRequestedEventListener paymentRequestedEventListener;

    @Test
    void 결제_요청_이벤트를_주문_결제_명령으로_변환한다() {
        // given
        final PaymentRequestedEvent event = new PaymentRequestedEvent(
                1L,
                OrderType.PURCHASE,
                2L,
                new BigDecimal("10000.00")
        );

        // when
        paymentRequestedEventListener.handle(event);

        // then
        verify(paymentService).payOrder(new PayOrderCommand(
                2L,
                1L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        ));
    }
}
