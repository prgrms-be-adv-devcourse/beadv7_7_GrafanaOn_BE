package shop.dear.commerce.financial.payment.domain.model;

import org.junit.jupiter.api.Test;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.commerce.financial.payment.domain.constant.PaymentPurpose;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentTest {

    @Test
    void createTopUp_success() {
        // given
        Long memberId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");

        // when
        Payment payment = Payment.createTopUp(memberId, amount);

        // then
        assertEquals(memberId, payment.getMemberId());
        assertEquals(PaymentPurpose.TOPUP, payment.getPurpose());
        assertNull(payment.getOrderId());
        assertNull(payment.getOrderType());
        assertEquals(amount, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getState());
    }

    @Test
    void createOrderPayment_success() {
        // given
        Long memberId = 1L;
        Long orderId = 100L;
        BigDecimal amount = new BigDecimal("15000.00");

        // when
        Payment payment = Payment.createOrderPayment(
                memberId,
                orderId,
                OrderType.PURCHASE,
                amount
        );

        // then
        assertEquals(memberId, payment.getMemberId());
        assertEquals(PaymentPurpose.ORDER, payment.getPurpose());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(OrderType.PURCHASE, payment.getOrderType());
        assertEquals(amount, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getState());
    }

    @Test
    void createOfferOrderPayment_success() {
        // given
        Long memberId = 1L;
        Long orderId = 200L;
        BigDecimal amount = new BigDecimal("15000.00");

        // when
        Payment payment = Payment.createOrderPayment(
                memberId,
                orderId,
                OrderType.OFFER,
                amount
        );

        // then
        assertEquals(memberId, payment.getMemberId());
        assertEquals(PaymentPurpose.ORDER, payment.getPurpose());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(OrderType.OFFER, payment.getOrderType());
        assertEquals(amount, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getState());
    }

    @Test
    void createTopUp_withNullMemberId_throwsException() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createTopUp(null, new BigDecimal("10000.00"))
        );

        assertEquals(PaymentErrorCode.INVALID_MEMBER_ID, exception.getErrorCode());
    }

    @Test
    void createTopUp_withZeroAmount_throwsException() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createTopUp(1L, BigDecimal.ZERO)
        );

        // then
        assertEquals(PaymentErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    @Test
    void createTopUp_withNullAmount_throwsException() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createTopUp(1L, null)
        );

        // then
        assertEquals(PaymentErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    @Test
    void createTopUp_withNegativeAmount_throwsException() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createTopUp(1L, new BigDecimal("-1.00"))
        );

        // then
        assertEquals(PaymentErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    @Test
    void createTopUp_withAmountExceedingTwoDecimalPlaces_throwsException() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createTopUp(1L, new BigDecimal("100.001"))
        );

        // then
        assertEquals(PaymentErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    @Test
    void createOrderPayment_withNullOrderId_throwsException() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createOrderPayment(
                        1L,
                        null,
                        OrderType.PURCHASE,
                        new BigDecimal("10000.00")
                )
        );

        assertEquals(PaymentErrorCode.INVALID_ORDER_REFERENCE, exception.getErrorCode());
    }

    @Test
    void createOrderPayment_withNullOrderType_throwsException() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> Payment.createOrderPayment(
                        1L,
                        100L,
                        null,
                        new BigDecimal("10000.00")
                )
        );

        assertEquals(PaymentErrorCode.INVALID_ORDER_REFERENCE, exception.getErrorCode());
    }

    @Test
    void preparePgPayment_success() {
        // given
        Long memberId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");
        Payment payment = Payment.createTopUp(memberId, amount);

        // when
        payment.preparePgPayment();

        // then
        PGPayment pgPayment = payment.getPgPayment();
        assertNotNull(pgPayment);
        assertSame(payment, pgPayment.getPayment());
        assertEquals(PGPaymentStatus.READY, pgPayment.getState());
        assertNull(pgPayment.getTransactionKey());
        assertNull(pgPayment.getApprovedAmount());
    }

    @Test
    void preparePgPayment_forOrderPayment_throwsException() {
        // given
        Payment payment = Payment.createOrderPayment(
                1L,
                100L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                payment::preparePgPayment
        );

        // then
        assertEquals(
                PaymentErrorCode.INVALID_PAYMENT_PURPOSE,
                exception.getErrorCode()
        );
        assertNull(payment.getPgPayment());
    }

    @Test
    void preparePgPayment_twice_throwsException() {
        // given
        Payment payment = Payment.createTopUp(
                1L,
                new BigDecimal("10000.00")
        );
        payment.preparePgPayment();

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                payment::preparePgPayment
        );

        // then
        assertEquals(
                PaymentErrorCode.PG_PAYMENT_ALREADY_PREPARED,
                exception.getErrorCode()
        );
    }

    @Test
    void complete_success() {
        // given
        Payment payment = Payment.createOrderPayment(
                1L,
                100L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );

        // when
        payment.complete();

        // then
        assertEquals(PaymentStatus.PAID, payment.getState());
    }

    @Test
    void complete_twice_throwsException() {
        // given
        Payment payment = Payment.createOrderPayment(
                1L,
                100L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );
        payment.complete();

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> payment.complete()
        );

        // then
        assertEquals(
                PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                exception.getErrorCode()
        );
    }

    @Test
    void pendingPayment_canFail() {
        final Payment payment = Payment.createOrderPayment(
                1L,
                10L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );

        payment.fail();

        assertEquals(PaymentStatus.FAILED, payment.getState());
    }

    @Test
    void fail_afterPaid_throwsException() {
        final Payment payment = Payment.createOrderPayment(
                1L,
                100L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );
        payment.complete();

        final BusinessException exception = assertThrows(
                BusinessException.class,
                payment::fail
        );

        assertEquals(
                PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                exception.getErrorCode()
        );
        assertEquals(PaymentStatus.PAID, payment.getState());
    }

    @Test
    void fail_twice_throwsException() {
        final Payment payment = Payment.createOrderPayment(
                1L,
                100L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );
        payment.fail();

        final BusinessException exception = assertThrows(
                BusinessException.class,
                payment::fail
        );

        assertEquals(
                PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                exception.getErrorCode()
        );
        assertEquals(PaymentStatus.FAILED, payment.getState());
    }
}
