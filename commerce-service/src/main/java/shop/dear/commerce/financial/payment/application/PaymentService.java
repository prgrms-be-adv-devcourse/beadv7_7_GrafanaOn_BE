package shop.dear.commerce.financial.payment.application;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.commerce.financial.payment.application.dto.PaymentInfo;
import shop.dear.commerce.financial.payment.application.event.WalletDebitRequestedEvent;
import shop.dear.commerce.financial.payment.application.port.PaymentCompletedEventPublisher;
import shop.dear.commerce.financial.payment.application.port.PaymentFailedEventPublisher;
import shop.dear.commerce.financial.payment.application.port.WalletDebitEventPublisher;
import shop.dear.commerce.financial.payment.domain.constant.PaymentPurpose;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.commerce.financial.payment.domain.model.Payment;
import shop.dear.commerce.financial.payment.domain.repository.PaymentRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletDebitEventPublisher walletDebitEventPublisher;
    private final PaymentCompletedEventPublisher paymentCompletedEventPublisher;
    private final PaymentFailedEventPublisher paymentFailedEventPublisher;

    @Transactional
    public PaymentInfo payOrder(final PayOrderCommand command) {
        final Payment payment = Payment.createOrderPayment(
                command.memberId(),
                command.orderId(),
                command.orderType(),
                command.amount()
        );

        final Payment savedPayment = paymentRepository.save(payment);

        walletDebitEventPublisher.publish(
                new WalletDebitRequestedEvent(
                        savedPayment.getId(),
                        savedPayment.getMemberId(),
                        savedPayment.getAmount(),
                        savedPayment.getOrderType()
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
                        payment.getOrderType(),
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
                        payment.getOrderType(),
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
}
