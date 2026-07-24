package shop.dear.commerce.order.offer.presentation;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dear.commerce.order.offer.application.OfferService;
import shop.dear.commerce.order.offer.presentation.dto.OfferStatusResponse;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/offers")
@RestController
public class OfferController {

  private final OfferService offerService;

  @GetMapping("/{productId}/status")
  public ResponseEntity<ApiResponse<OfferStatusResponse>> getOfferStatus(
      @Positive @PathVariable final Long productId
  ) {
    final boolean exists = offerService.existsActiveOfferByProductId(productId);
    return ResponseEntity.ok(successWithData(OfferStatusResponse.from(exists)));
  }

  @PatchMapping("/{offerId}/accept")
  public ResponseEntity<ApiResponse<Void>> acceptOffer(
      @PathVariable final Long offerId
  ) {
    offerService.acceptOffer(offerId);
    return ResponseEntity.ok(successWithData(null));
  }
}
