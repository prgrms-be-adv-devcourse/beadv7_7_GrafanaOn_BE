package shop.dear.commerce.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.presentation.dto.response.GetMemberProductExistsResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/internal/products")
@RestController
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/me/exists")
    public ResponseEntity<ApiResponse<GetMemberProductExistsResponse>> getMemberProductExists(@AuthUser final Long sellerId) {
        final MemberProductExistsDto result = productService.getMemberProductExists(sellerId);
        final GetMemberProductExistsResponse response = GetMemberProductExistsResponse.of(result);

        return ResponseEntity.ok(successWithData(response));
    }
}
