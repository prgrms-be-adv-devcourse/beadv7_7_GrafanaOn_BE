package shop.dear.commerce.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.commerce.product.application.dto.GetProductDetailDto;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.application.dto.ScrapProductInfoDto;
import shop.dear.commerce.product.application.dto.command.GetScrapProductCommand;
import shop.dear.commerce.product.presentation.dto.request.GetScrapProductsRequest;
import shop.dear.commerce.product.presentation.dto.response.GetMemberProductExistsResponse;
import shop.dear.commerce.product.presentation.dto.response.GetScrapProductResponse;
import shop.dear.commerce.product.presentation.dto.response.ProductDetailResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import java.util.List;

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

    @PostMapping("/me/scraps")
    public ResponseEntity<ApiResponse<List<GetScrapProductResponse>>> getScrapProducts(@AuthUser final Long memberId, @RequestBody final GetScrapProductsRequest request) {
        final GetScrapProductCommand command = request.toCommand();
        final List<ScrapProductInfoDto> result = productService.getScrapProducts(memberId, command);
        final List<GetScrapProductResponse> response = result.stream()
            .map(GetScrapProductResponse::of)
            .toList();

        return ResponseEntity.ok(successWithData(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@AuthUser final Long memberId, @PathVariable final Long productId) {
        final GetProductDetailDto result = productService.getProductDetailToInternal(memberId, productId);
        final ProductDetailResponse response = ProductDetailResponse.of(result);

        return ResponseEntity.ok(successWithData(response));
    }
}
