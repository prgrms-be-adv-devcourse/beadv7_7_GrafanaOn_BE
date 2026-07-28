package shop.dear.commerce.order.offer.presentation.dto;

import shop.dear.commerce.order.offer.domain.model.Offer;

import java.math.BigDecimal;

public record OfferListResponse(
        Long id,
        String number,
        String status,
        String paymentStatus,
        Long productId,
        Long snapshotId,
        BigDecimal amount,
        String title,
        String story,
        String delivery
) {

    public static OfferListResponse from(final Offer offer) {
        return new OfferListResponse(
                offer.getId(),
                offer.getNumber(),
                offer.getStatus().name(),
                offer.getPaymentStatus().name(),
                offer.getProductId(),
                offer.getSnapshotId(),
                offer.getAmount(),
                offer.getTitle(),
                offer.getStory(),
                offer.getDelivery()
        );
    }
}
