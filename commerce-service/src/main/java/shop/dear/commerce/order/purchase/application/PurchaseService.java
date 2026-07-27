package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseService {

    private static final long PAYMENT_DUE_MINUTES = 5;

    private final PurchaseRepository purchaseRepository;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final ProductPort productPort;

    public Purchase getPurchase(final Long purchaseId, final Long buyerId) {
        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        if (!Objects.equals(buyerId, purchase.getBuyerId())) {
            throw new BusinessException(PURCHASE_NOT_FOUND);
        }

        return purchase;
    }

    public List<Purchase> getPurchasesByBuyerId(final Long buyerId) {
        return purchaseRepository.findByBuyerId(buyerId);
    }

    @Transactional
    public Purchase createPurchase(final CreatePurchaseCommand command) {
        final ProductInfo product = productPort.getProduct(command.productId());
        validateProduct(command, product);
        final OffsetDateTime paymentDueAt = OffsetDateTime.now().plusMinutes(PAYMENT_DUE_MINUTES);

        final Purchase purchase = Purchase.create(
                command.buyerId(),
                product.sellerId(),
                product.productId(),
                product.price(),
                command.delivery(),
                paymentDueAt
        );

        return purchaseRepository.save(purchase);
    }

    private void validateProduct(
            final CreatePurchaseCommand command,
            final ProductInfo product
    ) {
        validateProductResponse(command.productId(), product);
        validateProductStatus(product);
        validateProductSaleType(product);
        validateProductOwner(command.buyerId(), product);
    }

    private void validateProductResponse(
            final Long productId,
            final ProductInfo product
    ) {
        if (product == null
                || !Objects.equals(productId, product.productId())
                || product.sellerId() == null
                || product.price() == null
                || product.price().compareTo(BigDecimal.ZERO) <= 0
                || product.saleType() == null
                || product.status() == null) {
            throw new BusinessException(INVALID_PRODUCT_RESPONSE);
        }
    }

    private void validateProductStatus(final ProductInfo product) {
        if (product.status() != ProductStatus.ON_SALE) {
            throw new BusinessException(PRODUCT_NOT_ON_SALE);
        }
    }

    private void validateProductSaleType(final ProductInfo product) {
        if (product.saleType() != ProductSaleType.IMMEDIATE) {
            throw new BusinessException(PRODUCT_NOT_FOR_IMMEDIATE_PURCHASE);
        }
    }

    private void validateProductOwner(
            final Long buyerId,
            final ProductInfo product
    ) {
        if (Objects.equals(buyerId, product.sellerId())) {
            throw new BusinessException(CANNOT_PURCHASE_OWN_PRODUCT);
        }
    }

    @Transactional
    public void cancelPurchase(final Long purchaseId, final Long buyerId) {
        final Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(PURCHASE_NOT_FOUND));

        validateBuyer(purchaseId, buyerId, purchase);
        purchase.cancel();
    }

    private void validateBuyer(final Long purchaseId, final Long buyerId, final Purchase purchase) {
        if (!Objects.equals(buyerId, purchase.getBuyerId())) {
            throw new BusinessException(PURCHASE_NOT_FOUND);
        }
    }

    @Transactional
    public void confirmPurchase(final Long purchaseId, final Long memberId) {
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
