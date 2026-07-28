package shop.dear.commerce.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.commerce.product.application.dto.GetProductDetailDto;
import shop.dear.commerce.product.application.dto.GetProductDto;
import shop.dear.commerce.product.application.dto.GetSellerProductDto;
import shop.dear.commerce.product.application.dto.PresignedUrlInfoDto;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.command.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.presentation.dto.request.CreateProductRequest;
import shop.dear.commerce.product.presentation.dto.request.GeneratePresignedUrlsRequest;
import shop.dear.commerce.product.presentation.dto.request.UpdateProductRequest;
import shop.dear.commerce.product.presentation.dto.response.GetProductResponse;
import shop.dear.commerce.product.presentation.dto.response.GetSellerProductResponse;
import shop.dear.commerce.product.presentation.dto.response.PresignedUrlsResponse;
import shop.dear.commerce.product.presentation.dto.response.ProductDetailResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import java.time.LocalDate;
import java.util.List;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/api/products")
@RestController
public class ProductController {

    private final ProductService productService;

    @PostMapping("/images/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlsResponse>> generatePresignedUrls(@AuthUser final Long memberId, @RequestBody GeneratePresignedUrlsRequest request) {
        final GeneratePresignedUrlsCommand command = request.toCommand();
        final List<PresignedUrlInfoDto> presignedUrls = productService.generatePresignedUrls(memberId, command);
        final PresignedUrlsResponse response = PresignedUrlsResponse.of(presignedUrls);

        return ResponseEntity.ok(successWithData(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProduct(@AuthUser final Long memberId, @RequestBody CreateProductRequest request) {
        final CreateProductCommand command = request.toCommand();
        productService.createProduct(memberId, command);

        return ResponseEntity.ok(success());
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
        @AuthUser final Long sellerId,
        @PathVariable Long productId,
        @RequestBody UpdateProductRequest request
    ) {
        final UpdateProductCommand command = request.toCommand();
        productService.updateProduct(sellerId, productId, command);

        return ResponseEntity.ok(success());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@AuthUser final Long sellerId, @PathVariable Long productId) {
        productService.deleteProduct(sellerId, productId);

        return ResponseEntity.ok(success());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@AuthUser final Long memberId, @PathVariable final Long productId) {
        final GetProductDetailDto result = productService.getProductDetail(memberId, productId);
        final ProductDetailResponse response = ProductDetailResponse.of(result);

        return ResponseEntity.ok(successWithData(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<GetSellerProductResponse>>> getSellerProducts(@AuthUser final Long sellerId) {
        final List<GetSellerProductDto> result = productService.getSellerProducts(sellerId);
        final List<GetSellerProductResponse> response = result.stream()
            .map(GetSellerProductResponse::of)
            .toList();

        return ResponseEntity.ok(successWithData(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GetProductResponse>>> getAllProduct(
        @RequestParam(value = "saleType",required = false) final ProductSaleType saleType,
        @RequestParam(value = "status", required = false) final ProductStatus status,
        @RequestParam(value = "createdAt", required = false) final LocalDate createdAt
    ) {
        final List<GetProductDto> result = productService.getAllProduct(saleType, status, createdAt);

        final List<GetProductResponse> response = result.stream()
            .map(GetProductResponse::of)
            .toList();

        return ResponseEntity.ok(successWithData(response));
    }
}
