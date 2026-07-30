package shop.dear.commerce.cart.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.cart.application.CartService;
import shop.dear.commerce.cart.application.dto.GetAllCartItemProductResponse;
import shop.dear.commerce.cart.infrastructure.event.CartItemAddRequestedEvent;
import shop.dear.commerce.cart.presentation.dto.request.AddCartItemRequest;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import java.util.List;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@RestController
public class CartController {

    private final CartService cartService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<ApiResponse<GetAllCartItemProductResponse>> findAllCartItems(
            @AuthUser final Long memberId
    ) {
        final GetAllCartItemProductResponse cartItems = cartService.getCartItems(memberId);
        return ResponseEntity.ok(successWithData(cartItems));
    }

    @DeleteMapping("/items/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllCartItems(
            @AuthUser final Long memberId
    ) {
        cartService.deleteAllCartItems(memberId);
        return ResponseEntity.ok(success());
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> deleteSelectedCartItems(
            @AuthUser final Long memberId,
            @RequestParam final List<Long> productIds
    ) {
        cartService.deleteSelectedCartItemsByUser(memberId, productIds);
        return ResponseEntity.ok(success());
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addCartItem(
            @AuthUser final Long memberId,
            @RequestBody final AddCartItemRequest request
    ) {
        eventPublisher.publishEvent(new CartItemAddRequestedEvent(memberId, request.productId()));
        return ResponseEntity.ok(success());
    }

}
