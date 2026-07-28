package shop.dear.commerce.financial.payment.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.commerce.financial.payment.application.dto.PaymentInfo;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.WalletDebitEventPublisher;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final Long PAYMENT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");

    private static final Long OTHER_MEMBER_ID = 2L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletDebitEventPublisher walletDebitEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void payOrder_savesPendingPayment_andPublishesWalletDebitRequest() {
        // given
        final PayOrderCommand command = new PayOrderCommand(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );

        final Payment savedPayment = mock(Payment.class);
        when(savedPayment.getId()).thenReturn(PAYMENT_ID);
        when(savedPayment.getMemberId()).thenReturn(MEMBER_ID);
        when(savedPayment.getAmount()).thenReturn(AMOUNT);
        when(savedPayment.getOrderType()).thenReturn(OrderType.PURCHASE);
        when(savedPayment.getState()).thenReturn(PaymentStatus.PENDING);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // when
        final PaymentInfo result = paymentService.payOrder(command);

        // then
        assertEquals(PAYMENT_ID.longValue(), result.paymentId().longValue());
        assertEquals(PaymentStatus.PENDING, result.state());

        final ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        final Payment requestedPayment = paymentCaptor.getValue();
        assertEquals(
                MEMBER_ID.longValue(),
                requestedPayment.getMemberId().longValue()
        );
        assertEquals(
                ORDER_ID.longValue(),
                requestedPayment.getOrderId().longValue()
        );
        assertEquals(OrderType.PURCHASE, requestedPayment.getOrderType());
        assertEquals(AMOUNT, requestedPayment.getAmount());
        assertEquals(PaymentStatus.PENDING, requestedPayment.getState());

        final ArgumentCaptor<WalletDebitRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(WalletDebitRequestedEvent.class);
        verify(walletDebitEventPublisher).publish(eventCaptor.capture());

        final WalletDebitRequestedEvent event = eventCaptor.getValue();
        assertEquals(PAYMENT_ID.longValue(), event.paymentId().longValue());
        assertEquals(MEMBER_ID.longValue(), event.memberId().longValue());
        assertEquals(AMOUNT, event.amount());
        assertEquals(OrderType.PURCHASE, event.orderType());
    }

    @Test
    void completePayment_changesPendingPaymentToPaid() {
        // given
        final Payment payment = Payment.createOrderPayment(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        paymentService.completePayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.PAID, payment.getState());
    }

    @Test
    void failPayment_changesPendingPaymentToFailed() {
        // given
        final Payment payment = Payment.createOrderPayment(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        paymentService.failPayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.FAILED, payment.getState());
    }

    @Test
    void completePayment_whenPaymentNotFound_throwsException() {
        // given
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.completePayment(PAYMENT_ID)
        );

        // then
        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void failPayment_whenPaymentNotFound_throwsException() {
        // given
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.failPayment(PAYMENT_ID)
        );

        // then
        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getPayment_whenOwner_returnsPaymentInfo() {
        // given
        final Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(PAYMENT_ID);
        when(payment.getMemberId()).thenReturn(MEMBER_ID);
        when(payment.getState()).thenReturn(PaymentStatus.PENDING);
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        final PaymentInfo result = paymentService.getPayment(
                MEMBER_ID,
                PAYMENT_ID
        );

        // then
        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentStatus.PENDING, result.state());
    }

    @Test
    void getPayment_whenDifferentMember_throwsAccessDeniedException() {
        // given
        final Payment payment = Payment.createOrderPayment(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.getPayment(OTHER_MEMBER_ID, PAYMENT_ID)
        );

        // then
        assertEquals(
                PaymentErrorCode.PAYMENT_ACCESS_DENIED,
                exception.getErrorCode()
        );
    }

    @Test
    void getPayment_whenPaymentNotFound_throwsException() {
        // given
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.getPayment(MEMBER_ID, PAYMENT_ID)
        );

        // then
        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());
    }
}
