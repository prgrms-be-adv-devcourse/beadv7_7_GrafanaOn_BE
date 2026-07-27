package shop.dear.commerce.order.offer.presentation.dto;

import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;

import java.math.BigDecimal;

public record CreateOfferSnapshotResponse(
        Long snapshotId,
        Long productId,
        String modelNumberSnapshot,
        BigDecimal priceSnapshot
) {

    public static CreateOfferSnapshotResponse from(final OfferSnapshot snapshot) {
        return new CreateOfferSnapshotResponse(
                snapshot.getId(),
                snapshot.getProductId(),
                snapshot.getModelNumberSnapshot(),
                snapshot.getPriceSnapshot()
        );
    }
}
