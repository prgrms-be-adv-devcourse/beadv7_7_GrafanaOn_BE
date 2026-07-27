package shop.dear.commerce.order.purchase.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.order.purchase.application.PurchaseService;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.presentation.dto.CreatePurchaseRequest;
import shop.dear.commerce.order.purchase.presentation.dto.PurchaseResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import java.util.List;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/api/purchases")
@RestController
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping("/{purchaseId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getPurchase(
            @Positive @PathVariable final Long purchaseId,
            @AuthUser final Long buyerId
    ) {
        final Purchase purchase = purchaseService.getPurchase(purchaseId, buyerId);
        final PurchaseResponse response = PurchaseResponse.from(purchase);
        return ResponseEntity.ok(successWithData(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getPurchasesByMe(
            @AuthUser final Long buyerId
    ) {
        final List<Purchase> purchases = purchaseService.getPurchasesByBuyerId(buyerId);
        final List<PurchaseResponse> responses = purchases.stream()
                .map(PurchaseResponse::from)
                .toList();
        return ResponseEntity.ok(successWithData(responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponse>> createPurchase(
            @Valid @RequestBody final CreatePurchaseRequest request,
            @AuthUser final Long buyerId
    ) {
        final Purchase purchase = purchaseService.createPurchase(request.toCommand(buyerId));
        final PurchaseResponse response = PurchaseResponse.from(purchase);
        return ResponseEntity.status(HttpStatus.CREATED).body(successWithData(response));
    }

    @DeleteMapping("/{purchaseId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPurchase(
            @Positive @PathVariable final Long purchaseId,
            @AuthUser final Long buyerId
    ) {
        purchaseService.cancelPurchase(purchaseId, buyerId);
        return ResponseEntity.ok(success());
    }

    @PostMapping("/{purchaseId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPurchase(
            @Positive @PathVariable final Long purchaseId,
            @AuthUser final Long memberId
    ) {
        purchaseService.confirmPurchase(purchaseId, memberId);
        return ResponseEntity.ok(success());
    }
}
