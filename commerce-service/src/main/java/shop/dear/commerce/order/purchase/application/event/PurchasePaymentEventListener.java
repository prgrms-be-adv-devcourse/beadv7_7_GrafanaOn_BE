package shop.dear.commerce.order.purchase.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.financial.PaymentCompletedEvent;
import shop.dear.common.event.financial.PaymentFailedEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PURCHASE_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class PurchasePaymentEventListener {

    private final PurchaseRepository purchaseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        if (event.orderType() != OrderType.PURCHASE) {
            return;
        }

        findPurchase(event.orderId()).pay();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(final PaymentFailedEvent event) {
        if (event.orderType() != OrderType.PURCHASE) {
            return;
        }

        findPurchase(event.orderId()).failPayment();
    }

    private Purchase findPurchase(final Long purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));
    }
}
