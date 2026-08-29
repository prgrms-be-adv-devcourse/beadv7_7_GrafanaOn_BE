package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.MemberPort;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.CanceledPurchaseEvent;
import shop.dear.common.type.OrderType;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseCancelReason;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseService {

    private static final long PAYMENT_DUE_MINUTES = 5;

    private final PurchaseRepository purchaseRepository;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final ProductPort productPort;
    private final MemberPort memberPort;
    private final PurchaseExpirationProcessor purchaseExpirationProcessor;

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

    public Page<Purchase> getPurchasesByBuyerId(final Long buyerId, final Pageable pageable) {
        validateMemberExists(buyerId);

        return purchaseRepository.findByBuyerId(buyerId, pageable);
    }

    // Product 상태(ON_SALE 등)를 조회/검증만 하는 단계. 아직 Product 상태를 변경하지 않으므로 실패해도 보상 불필요
    public ProductInfo validateAndGetProduct(final Long buyerId, final Long productId) {
        validateMemberExists(buyerId);

        final ProductInfo product = productPort.getProduct(productId);
        validateProductForPurchase(buyerId, product);

        return product;
    }

    // productPort.tradeProduct()로 이미 TRADING 전환에 성공한 이후 호출되는 단계.
    // 이 메서드 실행 중 실패하면 PurchaseFacade가 Product 상태 보상(outbox 기록)을 수행함
    @Transactional
    public Purchase createPurchase(final CreatePurchaseCommand command, final ProductInfo product) {
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
                OrderType.PURCHASE.name(),
                savedPurchase.getBuyerId(),
                savedPurchase.getAmount()
        ));

        return savedPurchase;
    }

    private void validateProductForPurchase(Long buyerId, ProductInfo product) {
        if (product.isOwnedBy(buyerId)) {
            throw new BusinessException(CANNOT_PURCHASE_OWN_PRODUCT);
        }

        if (!product.isOnSale()) {
            throw new BusinessException(PRODUCT_NOT_ON_SALE);
        }

        if (!product.isImmediateSale()) {
            throw new BusinessException(PRODUCT_NOT_FOR_IMMEDIATE_PURCHASE);
        }
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

        purchaseEventPublisher.publish(new CanceledPurchaseEvent(
                purchase.getId(),
                purchase.getBuyerId(),
                purchase.getSellerId(),
                purchase.getProductId(),
                PurchaseCancelReason.CANCELLED.name()
        ));
    }

    public void expireOverduePurchases() {
        final LocalDateTime now = LocalDateTime.now();
        final List<Purchase> overduePurchases = purchaseRepository.findAllByStatusAndPaymentDueAtBefore(
                PurchaseStatus.PENDING_PAYMENT, now);

        int successCount = 0;
        int skippedCount = 0;
        final List<Long> failedPurchaseIds = new ArrayList<>();

        for (final Purchase purchase : overduePurchases) {
            try {
                final boolean expired = purchaseExpirationProcessor.expire(purchase.getId());
                if (expired) {
                    successCount++;
                } else {
                    skippedCount++;
                }
            } catch (final Exception e) {
                log.error("구매 만료 처리 중 예외가 발생하여 건너뜁니다. purchaseId={}", purchase.getId(), e);
                failedPurchaseIds.add(purchase.getId());
            }
        }

        log.info("구매 만료 스케줄러 실행 완료: 대상={}건, 성공={}건, 스킵(상태 변경됨)={}건, 실패={}건, 실패 ID={}",
                overduePurchases.size(), successCount, skippedCount, failedPurchaseIds.size(), failedPurchaseIds);
    }
}
