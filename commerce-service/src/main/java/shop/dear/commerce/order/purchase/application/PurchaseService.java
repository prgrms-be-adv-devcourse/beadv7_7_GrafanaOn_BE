package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.MemberPort;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PURCHASE_NOT_FOUND;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseService {

    private static final long PAYMENT_DUE_MINUTES = 5;

    private final PurchaseRepository purchaseRepository;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final ProductPort productPort;
    private final MemberPort memberPort;

    public Purchase getPurchase(final Long purchaseId, final Long buyerId) {
        validateMemberExists(buyerId);

        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        if (!Objects.equals(buyerId, purchase.getBuyerId())) {
            throw new BusinessException(PURCHASE_NOT_FOUND);
        }

        return purchase;
    }

    private void validateMemberExists(final Long memberId) {
        memberPort.validateMemberExists(memberId);
    }

    public List<Purchase> getPurchasesByBuyerId(final Long buyerId) {
        validateMemberExists(buyerId);

        return purchaseRepository.findByBuyerId(buyerId);
    }

    @Transactional
    public Purchase createPurchase(final CreatePurchaseCommand command) {
        validateMemberExists(command.buyerId());

        final ProductInfo product = productPort.getProduct(command.productId());
        final LocalDateTime paymentDueAt =
                LocalDateTime.now().plusMinutes(PAYMENT_DUE_MINUTES);

        final Purchase purchase = Purchase.create(
                command.buyerId(),
                product.sellerId(),
                command.productId(),
                product.price(),
                command.delivery(),
                paymentDueAt
        );

        final Purchase savedPurchase = purchaseRepository.save(purchase);

        purchaseEventPublisher.publish(new PaymentRequestedEvent(
                savedPurchase.getId(),
                OrderType.PURCHASE,
                savedPurchase.getBuyerId(),
                savedPurchase.getAmount()
        ));

        return savedPurchase;
    }

    @Transactional
    public void cancelPurchase(final Long purchaseId, final Long buyerId) {
        validateMemberExists(buyerId);

        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        if (!Objects.equals(buyerId, purchase.getBuyerId())) {
            throw new BusinessException(PURCHASE_NOT_FOUND);
        }

        purchase.cancel();
    }

    @Transactional
    public void confirmPurchase(final Long purchaseId, final Long memberId) {
        validateMemberExists(memberId);

        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        if (!Objects.equals(memberId, purchase.getBuyerId())) {
            throw new BusinessException(PURCHASE_NOT_FOUND);
        }

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
