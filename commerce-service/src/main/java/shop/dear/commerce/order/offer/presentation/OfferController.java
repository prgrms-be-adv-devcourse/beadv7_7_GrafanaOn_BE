package shop.dear.commerce.order.offer.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dear.commerce.order.offer.application.OfferService;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.presentation.dto.CreateOfferRequest;
import shop.dear.commerce.order.offer.presentation.dto.CreateOfferSnapshotRequest;
import shop.dear.commerce.order.offer.presentation.dto.CreateOfferSnapshotResponse;
import shop.dear.commerce.order.offer.presentation.dto.OfferResponse;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/offers")
@RestController
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/snapshot")
    public ResponseEntity<ApiResponse<CreateOfferSnapshotResponse>> createOfferSnapshot(
            @Valid @RequestBody final CreateOfferSnapshotRequest request,
            @AuthUser final Long memberId
    ) {
        final OfferSnapshot snapshot = offerService.createOfferSnapshot(request.toCommand(memberId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(successWithData(CreateOfferSnapshotResponse.from(snapshot)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(
            @Valid @RequestBody final CreateOfferRequest request,
            @AuthUser final Long memberId
    ) {
        final Offer offer = offerService.createOffer(request.toCommand(memberId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(successWithData(OfferResponse.from(offer)));
    }

    @PatchMapping("/{offerId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(
            @Positive @PathVariable final Long offerId,
            @AuthUser final Long memberId
    ) {
        offerService.acceptOffer(offerId, memberId);
        return ResponseEntity.ok(successWithData(null));
    }
}
