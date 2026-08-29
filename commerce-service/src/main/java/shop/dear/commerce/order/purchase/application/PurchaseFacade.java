package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.infrastructure.outbox.CompensationOutboxWriter;
import shop.dear.common.exception.BusinessException;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PRODUCT_ALREADY_TRADING;

// createPurchase의 트랜잭션 경계 밖(HTTP 호출)에서 일어나는 오케스트레이션과 보상 처리를 담당.
// PurchaseService는 순수 비즈니스 로직만 유지하고, try-catch/보상 로직은 이 계층에 모아둔다.
@Component
@RequiredArgsConstructor
public class PurchaseFacade {

    private final PurchaseService purchaseService;
    private final ProductPort productPort;
    private final CompensationOutboxWriter compensationOutboxWriter;

    public Purchase createPurchase(final CreatePurchaseCommand command) {
        final ProductInfo product = purchaseService.validateAndGetProduct(command.buyerId(), command.productId());

        if (!productPort.tradeProduct(command.productId())) {
            throw new BusinessException(PRODUCT_ALREADY_TRADING);
        }

        // 이 지점부터는 Product가 이미 TRADING으로 바뀐 상태이므로, 실패 시 반드시 보상 기록이 필요함
        try {
            return purchaseService.createPurchase(command, product);
        } catch (final RuntimeException e) {
            compensationOutboxWriter.recordTradeRevertFailure(command.productId());
            throw e;
        }
    }
}
