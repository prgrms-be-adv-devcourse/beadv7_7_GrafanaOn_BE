package shop.dear.commerce.order.offer.presentation.dto;

import shop.dear.commerce.order.offer.domain.model.Offer;

import java.math.BigDecimal;

public record OfferResponse(
        Long id,
        String number,
        String status,
        String paymentStatus,
        Long buyerId,
        Long sellerId,
        Long productId,
        Long snapshotId,
        BigDecimal amount,
        String title,
        String story,
        String delivery
) {

    public static OfferResponse from(final Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getNumber(),
                offer.getStatus().name(),
                offer.getPaymentStatus().name(),
                offer.getBuyerId(),
                offer.getSellerId(),
                offer.getProductId(),
                offer.getSnapshotId(),
                offer.getAmount(),
                offer.getTitle(),
                offer.getStory(),
                offer.getDelivery()
        );
    }
}
