package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.order.CanceledPurchaseEvent;
import shop.dear.common.event.order.ReleaseReason;
import shop.dear.common.exception.BusinessException;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PURCHASE_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class PurchaseExpirationProcessor {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseEventPublisher purchaseEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(final Long purchaseId) {
        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        final int updatedRows = purchaseRepository.updateStatusIfCurrent(
                purchaseId, PurchaseStatus.PENDING_PAYMENT, PurchaseStatus.EXPIRED);

        if (updatedRows == 0) {
            return false;
        }

        purchaseEventPublisher.publish(new CanceledPurchaseEvent(
                purchase.getId(),
                purchase.getBuyerId(),
                purchase.getSellerId(),
                purchase.getProductId(),
                ReleaseReason.EXPIRED
        ));

        return true;
    }
}
