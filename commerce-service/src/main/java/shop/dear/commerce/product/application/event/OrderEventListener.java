package shop.dear.commerce.product.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.common.event.order.CanceledPurchaseEvent;
import shop.dear.common.event.order.FinishedOrderEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ProductService productService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFinishedOrder(final FinishedOrderEvent event) {
        try {
            productService.completeProductSale(event.productId());
        } catch (Exception e) {
            log.error("주문 완료 후 상품 상태 변경 실패 - productId: {}", event.productId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCanceledPurchase(final CanceledPurchaseEvent event) {
        try {
            productService.canceledPurchase(event.productId());
        } catch (Exception e) {
            log.error("주문 취소 후 상품 상태 변경 실패 - productId: {}", event.productId(), e);
        }
    }
}
