package shop.dear.commerce.cart.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.cart.application.CartService;
import shop.dear.commerce.cart.application.dto.GetAllCartItemProductResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@RestController
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetAllCartItemProductResponse>> findAllCartItems(
            @AuthUser final Long memberId
    ) {
        final GetAllCartItemProductResponse cartItems = cartService.getCartItems(memberId);
        return ResponseEntity.ok(successWithData(cartItems));
    }

}
