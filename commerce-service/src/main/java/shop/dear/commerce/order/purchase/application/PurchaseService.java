package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PurchaseService {

  private final PurchaseRepository purchaseRepository;
  private final PurchaseEventPublisher purchaseEventPublisher;

  @Transactional
  public void confirmPurchase(final Long purchaseId) {
    final Purchase purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new BusinessException(PurchaseErrorCode.PURCHASE_NOT_FOUND));

    purchase.confirmPurchase();

    purchaseEventPublisher.publish(new FinishedOrderEvent(
        purchase.getId(),
        purchase.getBuyerId(),
        purchase.getSellerId(),
        purchase.getProductId(),
        purchase.getAmount(),
        OrderType.PURCHASE
    ));
  }
}
