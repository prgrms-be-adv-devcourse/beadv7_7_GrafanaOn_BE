package shop.dear.commerce.order.purchase.presentation.dto;

import shop.dear.commerce.order.purchase.domain.model.Purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseResponse(
    Long id,
    String number,
    String status,
    Long productId,
    BigDecimal amount,
    LocalDateTime purchasedAt,
    LocalDateTime paymentDueAt,
    String delivery
) {

    public static PurchaseResponse from(final Purchase purchase) {
        return new PurchaseResponse(
            purchase.getId(),
            purchase.getNumber(),
            purchase.getStatus().name(),
            purchase.getProductId(),
            purchase.getAmount(),
            purchase.getPurchasedAt(),
            purchase.getPaymentDueAt(),
            purchase.getDelivery()
        );
    }
}
