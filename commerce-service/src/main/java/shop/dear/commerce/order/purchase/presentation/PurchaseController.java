package shop.dear.commerce.order.purchase.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.order.purchase.application.PurchaseService;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.presentation.dto.CreatePurchaseRequest;
import shop.dear.commerce.order.purchase.presentation.dto.PurchaseResponse;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/api/purchases")
@RestController
public class PurchaseController {

    private final PurchaseService purchaseService;

    // TODO: 추후 JWT 파싱 구현 완료 시 커스텀 어노테이션으로 교체 예정
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponse>> createPurchase(
        @Valid @RequestBody final CreatePurchaseRequest request,
        @Positive @RequestHeader("X-Member-Id") final Long buyerId
    ) {
        final Purchase purchase = purchaseService.createPurchase(request.toCommand(buyerId));
        final PurchaseResponse response = PurchaseResponse.from(purchase);
        return ResponseEntity.status(HttpStatus.CREATED).body(successWithData(response));
    }
}
