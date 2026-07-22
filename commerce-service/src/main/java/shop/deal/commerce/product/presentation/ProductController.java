package shop.deal.commerce.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.deal.commerce.product.application.ProductService;
import shop.deal.commerce.product.application.dto.PresignedUrlInfo;
import shop.deal.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;
import shop.deal.commerce.product.presentation.dto.request.GeneratePresignedUrlsRequest;
import shop.deal.commerce.product.presentation.dto.response.PresignedUrlsResponse;
import shop.deal.common.response.ApiResponse;

import java.util.List;

import static shop.deal.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/api/products")
@RestController
public class ProductController {

    private final ProductService productService;

    // TODO: 추후 JWT 파싱 구현 완료 시 SecurityContext/Header 등에서 실제 memberId 추출하여 매핑해야 함
    @PostMapping("/images/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlsResponse>> generatePresignedUrls(final Long memberId, @RequestBody GeneratePresignedUrlsRequest request) {
        final GeneratePresignedUrlsCommand command = request.toCommand();
        final List<PresignedUrlInfo> presignedUrls = productService.generatePresignedUrls(memberId, command);
        final PresignedUrlsResponse response = PresignedUrlsResponse.of(presignedUrls);

        return ResponseEntity.ok(successWithData(response));
    }
}
