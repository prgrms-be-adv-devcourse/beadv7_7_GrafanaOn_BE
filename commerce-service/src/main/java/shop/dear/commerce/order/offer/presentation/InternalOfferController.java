package shop.dear.commerce.order.offer.presentation;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.order.offer.application.OfferService;
import shop.dear.commerce.order.offer.presentation.dto.OfferStatusResponse;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/internal/offers")
@RestController
public class InternalOfferController {

    private final OfferService offerService;

    @GetMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<OfferStatusResponse>> getOfferStatus(
            @Positive @PathVariable final Long productId
    ) {
        final boolean exists = offerService.existsActiveOfferByProductId(productId);
        return ResponseEntity.ok(successWithData(OfferStatusResponse.from(exists)));
    }
}
