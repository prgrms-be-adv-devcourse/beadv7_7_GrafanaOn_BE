package shop.dear.commerce.financial.payment.application;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import shop.dear.commerce.financial.payment.application.dto.*;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.PgPaymentApprovalPort;
import shop.dear.commerce.financial.payment.application.port.PaymentCompletedEventPublisher;
import shop.dear.commerce.financial.payment.application.port.PaymentFailedEventPublisher;
import shop.dear.commerce.financial.payment.application.port.WalletDebitEventPublisher;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.commerce.financial.payment.domain.constant.PaymentPurpose;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.type.OrderType;
import shop.dear.common.exception.BusinessException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletDebitEventPublisher walletDebitEventPublisher;
    private final PaymentCompletedEventPublisher paymentCompletedEventPublisher;
    private final PaymentFailedEventPublisher paymentFailedEventPublisher;
    private final PgPaymentApprovalPort pgPaymentApprovalPort;
    private final TopUpApprovalCompletionService topUpApprovalCompletionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentInfo payOrder(final PayOrderCommand command) {
        final OrderType orderType = OrderType.valueOf(command.orderType());

        final Optional<Payment> existingPayment =
                paymentRepository.findByOrderIdAndOrderType(
                        command.orderId(),
                        orderType
                );

        if (existingPayment.isPresent()) {
            return PaymentInfo.from(existingPayment.get());
        }

        final Payment payment = Payment.createOrderPayment(
                command.memberId(),
                command.orderId(),
                orderType,
                command.amount()
        );

        final Payment savedPayment = paymentRepository.save(payment);

        walletDebitEventPublisher.publish(
                new WalletDebitRequestedEvent(
                        savedPayment.getId(),
                        savedPayment.getMemberId(),
                        savedPayment.getAmount(),
                        savedPayment.getOrderType().name()
                )
        );

        return PaymentInfo.from(savedPayment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completePayment(final Long paymentId) {
        final Payment payment = findPayment(paymentId);

        payment.complete();

        if (payment.getPurpose() != PaymentPurpose.ORDER) {
            return;
        }

        paymentCompletedEventPublisher.publish(
                new PaymentCompletedEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getOrderType().name(),
                        payment.getMemberId(),
                        payment.getAmount(),
                        payment.getPaidAt()
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPayment(final Long paymentId) {
        final Payment payment = findPayment(paymentId);

        payment.fail();

        if (payment.getPurpose() != PaymentPurpose.ORDER) {
            return;
        }

        paymentFailedEventPublisher.publish(
                new PaymentFailedEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getOrderType().name(),
                        payment.getMemberId(),
                        payment.getAmount()
                )
        );
    }

    public PaymentInfo getPayment(
            final Long memberId,
            final Long paymentId
    ) {
        final Payment payment = findPayment(paymentId);

        if (!payment.getMemberId().equals(memberId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }

        return PaymentInfo.from(payment);
    }

    private Payment findPayment(final Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND)
                );
    }

    @Transactional
    public ChargeInfo prepareCharge(final ChargeCommand command) {
        final Payment payment = Payment.createTopUp(
                command.memberId(),
                command.amount()
        );

        final String merchantOrderId = "TOPUP_" + UUID.randomUUID();
        payment.preparePgPayment(merchantOrderId);

        final Payment savedPayment = paymentRepository.save(payment);

        return ChargeInfo.from(savedPayment);
    }

    // Toss HTTP 호출 중에는 DB 트랜잭션을 열지 않음
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void confirmCharge(final ConfirmChargeCommand command) {
        final Payment payment = paymentRepository.findByMerchantOrderId(
                        command.orderId()
                )
                .orElseThrow(() ->
                        new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND)
                );

        validateBeforeApproval(payment, command);

        final PgApprovalResult approvalResult = pgPaymentApprovalPort.approve(
                command.paymentKey(),
                command.orderId(),
                command.amount(),
                "topup-confirm-" + payment.getId()
        );

        validateApprovalResult(command, payment, approvalResult);

        topUpApprovalCompletionService.recordApproval(approvalResult);
    }

    private void validateBeforeApproval(
            final Payment payment,
            final ConfirmChargeCommand command
    ) {
        if (!payment.getMemberId().equals(command.memberId())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }

        if (payment.getPurpose() != PaymentPurpose.TOPUP) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_PURPOSE);
        }

        if (payment.getState() != PaymentStatus.PENDING
                || payment.getPgPayment().getState() != PGPaymentStatus.READY) {
            throw new BusinessException(
                    PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION
            );
        }

        if (payment.getAmount().compareTo(command.amount()) != 0) {
            throw new BusinessException(
                    PaymentErrorCode.PAYMENT_CONFIRMATION_MISMATCH
            );
        }
    }

    private void validateApprovalResult(
            final ConfirmChargeCommand command,
            final Payment payment,
            final PgApprovalResult approvalResult
    ) {
        if (approvalResult == null
                || !command.orderId().equals(approvalResult.orderId())
                || !command.paymentKey().equals(approvalResult.paymentKey())
                || approvalResult.approvedAmount() == null
                || payment.getAmount()
                .compareTo(approvalResult.approvedAmount()) != 0) {
            throw new BusinessException(
                    PaymentErrorCode.PAYMENT_CONFIRMATION_MISMATCH
            );
        }
    }
}
