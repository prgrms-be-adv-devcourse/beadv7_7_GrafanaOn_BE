package shop.dear.commerce.order.offer.presentation.dto;

import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;

public record CreateOfferSnapshotResponse(
        Long snapshotId
) {

    public static CreateOfferSnapshotResponse from(final OfferSnapshot snapshot) {
        return new CreateOfferSnapshotResponse(snapshot.getId());
    }
}
