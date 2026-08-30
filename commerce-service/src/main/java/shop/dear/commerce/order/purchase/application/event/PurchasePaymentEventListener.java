package shop.dear.commerce.order.purchase.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.common.event.FinishedOrderEventPublisher;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.event.order.CanceledPurchaseEvent;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.type.OrderType;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseCancelReason;
import shop.dear.common.exception.BusinessException;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PURCHASE_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchasePaymentEventListener {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final FinishedOrderEventPublisher finishedOrderEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        if (!OrderType.PURCHASE.name().equals(event.orderType())) {
            return;
        }
        final Purchase purchase = findPurchase(event.orderId());

        // TODO: 환불 로직 필요
        if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT) {
            log.error("결제 완료 시점에 구매가 이미 만료/취소됨. 환불 필요. purchaseId={}, status={}",
                    purchase.getId(), purchase.getStatus());
            return;
        }

        purchase.pay();

        final FinishedOrderEvent finishedOrderEvent = new FinishedOrderEvent(
                purchase.getId(),
                purchase.getBuyerId(),
                purchase.getSellerId(),
                purchase.getProductId(),
                purchase.getAmount(),
                OrderType.PURCHASE.name()
        );

        purchaseEventPublisher.publish(finishedOrderEvent);
        finishedOrderEventPublisher.publish(finishedOrderEvent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(final PaymentFailedEvent event) {
        if (!OrderType.PURCHASE.name().equals(event.orderType())) {
            return;
        }

        final Purchase purchase = findPurchase(event.orderId());
        purchase.failPayment();

        purchaseEventPublisher.publish(new CanceledPurchaseEvent(
                purchase.getId(),
                purchase.getBuyerId(),
                purchase.getSellerId(),
                purchase.getProductId(),
                PurchaseCancelReason.PAYMENT_FAILED.name()
        ));
    }

    private Purchase findPurchase(final Long purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));
    }
}
