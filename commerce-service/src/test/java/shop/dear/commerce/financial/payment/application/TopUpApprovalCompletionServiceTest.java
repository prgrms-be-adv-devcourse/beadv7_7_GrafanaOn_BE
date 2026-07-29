package shop.dear.commerce.financial.payment.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.financial.payment.application.dto.PgApprovalResult;
import shop.dear.commerce.financial.payment.application.event.WalletTopUpRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.WalletTopUpEventPublisher;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TopUpApprovalCompletionServiceTest {
    private static final Long PAYMENT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_ID = "TOPUP_test-order-001";
    private static final String TRANSACTION_KEY = "test-transaction-key";
    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletTopUpEventPublisher walletTopUpEventPublisher;

    @InjectMocks
    private TopUpApprovalCompletionService topUpApprovalCompletionService;

    @Test
    void recordApproval_marksPgPaymentDone_andPublishesTopUpEvent() {
        // given
        final Payment payment = Payment.createTopUp(MEMBER_ID, AMOUNT);
        payment.preparePgPayment(ORDER_ID);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

        final PgApprovalResult approvalResult = new PgApprovalResult(
                ORDER_ID,
                TRANSACTION_KEY,
                AMOUNT
        );

        when(paymentRepository.findByMerchantOrderId(ORDER_ID))
                .thenReturn(Optional.of(payment));

        // when
        topUpApprovalCompletionService.recordApproval(approvalResult);

        // then
        assertEquals(
                PGPaymentStatus.DONE,
                payment.getPgPayment().getState()
        );
        assertEquals(
                TRANSACTION_KEY,
                payment.getPgPayment().getTransactionKey()
        );
        assertEquals(
                AMOUNT,
                payment.getPgPayment().getApprovedAmount()
        );

        final ArgumentCaptor<WalletTopUpRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(WalletTopUpRequestedEvent.class);

        verify(walletTopUpEventPublisher).publish(eventCaptor.capture());

        final WalletTopUpRequestedEvent event = eventCaptor.getValue();
        assertEquals(PAYMENT_ID, event.paymentId());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(AMOUNT, event.amount());
    }
}
