package shop.dear.commerce.financial.payment.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.financial.payment.application.dto.*;
import shop.dear.commerce.financial.payment.application.port.PgPaymentApprovalPort;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.commerce.financial.payment.domain.constant.PaymentPurpose;
import shop.dear.commerce.financial.payment.application.port.PaymentCompletedEventPublisher;
import shop.dear.commerce.financial.payment.application.port.PaymentFailedEventPublisher;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.WalletDebitEventPublisher;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final Long PAYMENT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");

    private static final Long OTHER_MEMBER_ID = 2L;

    private static final String MERCHANT_ORDER_ID = "TOPUP_test-order-001";
    private static final String PAYMENT_KEY = "test-payment-key";
    private static final String TRANSACTION_KEY = "test-transaction-key";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletDebitEventPublisher walletDebitEventPublisher;

    @Mock
    private PaymentCompletedEventPublisher paymentCompletedEventPublisher;

    @Mock
    private PaymentFailedEventPublisher paymentFailedEventPublisher;

    @Mock
    private PgPaymentApprovalPort pgPaymentApprovalPort;

    @Mock
    private TopUpApprovalCompletionService topUpApprovalCompletionService;

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

        when(paymentRepository.findByOrderIdAndOrderType(
                ORDER_ID,
                OrderType.PURCHASE
        )).thenReturn(Optional.empty());

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
    void payOrder_whenOrderPaymentAlreadyExists_returnsExistingPaymentWithoutDebit() {
        // given
        final PayOrderCommand command = new PayOrderCommand(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );

        final Payment existingPayment = mock(Payment.class);
        when(existingPayment.getId()).thenReturn(PAYMENT_ID);
        when(existingPayment.getState()).thenReturn(PaymentStatus.PENDING);

        when(paymentRepository.findByOrderIdAndOrderType(
                ORDER_ID,
                OrderType.PURCHASE
        )).thenReturn(Optional.of(existingPayment));

        // when
        final PaymentInfo result = paymentService.payOrder(command);

        // then
        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentStatus.PENDING, result.state());

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(walletDebitEventPublisher);
    }

    @Test
    void completePayment_changesOrderPaymentToPaid_andPublishesEvent() {
        // given
        final Payment payment = Payment.createOrderPayment(
                MEMBER_ID,
                ORDER_ID,
                OrderType.PURCHASE,
                AMOUNT
        );
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        paymentService.completePayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.PAID, payment.getState());
        assertNotNull(payment.getPaidAt());

        final ArgumentCaptor<PaymentCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);

        verify(paymentCompletedEventPublisher).publish(eventCaptor.capture());

        final PaymentCompletedEvent event = eventCaptor.getValue();
        assertEquals(PAYMENT_ID, event.paymentId());
        assertEquals(ORDER_ID, event.orderId());
        assertEquals(OrderType.PURCHASE, event.orderType());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(AMOUNT, event.amount());
        assertEquals(payment.getPaidAt(), event.paidAt());
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

    @Test
    void completePayment_whenTopUp_doesNotPublishPaymentCompletedEvent() {
        // given
        final Payment payment = Payment.createTopUp(MEMBER_ID, AMOUNT);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        // when
        paymentService.completePayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.PAID, payment.getState());
        assertEquals(PaymentPurpose.TOPUP, payment.getPurpose());
        verifyNoInteractions(paymentCompletedEventPublisher);
    }

    @Test
    void failPayment_doesNotPublishPaymentCompletedEvent() {
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
        verifyNoInteractions(paymentCompletedEventPublisher);
    }

    @Test
    void prepareCharge_createsPendingTopUpPaymentWithReadyPgPayment() {
        // given
        final ChargeCommand command = new ChargeCommand(
                MEMBER_ID,
                AMOUNT
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    final Payment payment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            payment,
                            "id",
                            PAYMENT_ID
                    );
                    return payment;
                });

        // when
        final ChargeInfo result = paymentService.prepareCharge(command);

        // then
        final ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        final Payment savedPayment = paymentCaptor.getValue();

        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentPurpose.TOPUP, savedPayment.getPurpose());
        assertEquals(PaymentStatus.PENDING, savedPayment.getState());

        assertNotNull(savedPayment.getPgPayment());
        assertEquals(
                PGPaymentStatus.READY,
                savedPayment.getPgPayment().getState()
        );
        assertTrue(
                savedPayment.getPgPayment()
                        .getMerchantOrderId()
                        .startsWith("TOPUP_")
        );
        assertEquals(
                savedPayment.getPgPayment().getMerchantOrderId(),
                result.orderId()
        );
    }

    @Test
    void confirmCharge_whenValid_approvesPgAndRecordsApproval() {
        // given
        final Payment payment = createPreparedTopUpPayment();

        final ConfirmChargeCommand command = new ConfirmChargeCommand(
                MEMBER_ID,
                PAYMENT_KEY,
                MERCHANT_ORDER_ID,
                AMOUNT
        );

        final PgApprovalResult approvalResult = new PgApprovalResult(
                MERCHANT_ORDER_ID,
                TRANSACTION_KEY,
                AMOUNT
        );

        when(paymentRepository.findByMerchantOrderId(MERCHANT_ORDER_ID))
                .thenReturn(Optional.of(payment));

        when(pgPaymentApprovalPort.approve(
                PAYMENT_KEY,
                MERCHANT_ORDER_ID,
                AMOUNT,
                "topup-confirm-" + PAYMENT_ID
        )).thenReturn(approvalResult);

        // when
        paymentService.confirmCharge(command);

        // then
        verify(pgPaymentApprovalPort).approve(
                PAYMENT_KEY,
                MERCHANT_ORDER_ID,
                AMOUNT,
                "topup-confirm-" + PAYMENT_ID
        );
        verify(topUpApprovalCompletionService)
                .recordApproval(approvalResult);
    }

    @Test
    void confirmCharge_whenAmountDiffers_doesNotCallPg() {
        // given
        final Payment payment = createPreparedTopUpPayment();

        final ConfirmChargeCommand command = new ConfirmChargeCommand(
                MEMBER_ID,
                PAYMENT_KEY,
                MERCHANT_ORDER_ID,
                new BigDecimal("9999.00")
        );

        when(paymentRepository.findByMerchantOrderId(MERCHANT_ORDER_ID))
                .thenReturn(Optional.of(payment));

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.confirmCharge(command)
        );

        // then
        assertEquals(
                PaymentErrorCode.PAYMENT_CONFIRMATION_MISMATCH,
                exception.getErrorCode()
        );
        verifyNoInteractions(
                pgPaymentApprovalPort,
                topUpApprovalCompletionService
        );
    }

    @Test
    void confirmCharge_whenDifferentMember_doesNotCallPg() {
        // given
        final Payment payment = createPreparedTopUpPayment();

        final ConfirmChargeCommand command = new ConfirmChargeCommand(
                OTHER_MEMBER_ID,
                PAYMENT_KEY,
                MERCHANT_ORDER_ID,
                AMOUNT
        );

        when(paymentRepository.findByMerchantOrderId(MERCHANT_ORDER_ID))
                .thenReturn(Optional.of(payment));

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.confirmCharge(command)
        );

        // then
        assertEquals(
                PaymentErrorCode.PAYMENT_ACCESS_DENIED,
                exception.getErrorCode()
        );
        verifyNoInteractions(
                pgPaymentApprovalPort,
                topUpApprovalCompletionService
        );
    }

    private Payment createPreparedTopUpPayment() {
        final Payment payment = Payment.createTopUp(MEMBER_ID, AMOUNT);
        payment.preparePgPayment(MERCHANT_ORDER_ID);

        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

        return payment;
    }
}
