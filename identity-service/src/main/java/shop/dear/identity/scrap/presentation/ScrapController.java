package shop.dear.identity.scrap.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.pagination.PaginationRequest;
import shop.dear.common.pagination.PaginationResponse;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.scrap.application.ScrapService;
import shop.dear.identity.scrap.application.dto.ScrapDetail;
import shop.dear.identity.scrap.presentation.dto.ScrapListItemResponse;
import shop.dear.identity.scrap.presentation.dto.ScrapResponse;

import java.util.List;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@Validated
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<ScrapResponse>> addScrap(
        @PathVariable final Long productId,
        @AuthUser final Long memberId
    ) {
        ScrapResponse scrap = ScrapResponse.from(scrapService.addScrap(memberId, productId));

        return ResponseEntity.ok(successWithData(scrap));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<ScrapListItemResponse>>> getScrapList(
        @AuthUser final Long memberId,
        @RequestParam(required = false) final Integer page,
        @RequestParam(required = false) final Integer size
    ) {
        PaginationRequest paginationRequest = new PaginationRequest(page, size);

        Page<ScrapDetail> scraps = scrapService.getScrapList(
            memberId,
            paginationRequest.getPageNo() - 1,
            paginationRequest.getPageSize()
        );

        List<ScrapListItemResponse> content = scraps.getContent().stream()
            .map(ScrapListItemResponse::from)
            .toList();

        return ResponseEntity.ok(successWithData(PaginationResponse.of(scraps, content)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteScrap(
        @PathVariable final Long productId,
        @AuthUser final Long memberId
    ) {
        scrapService.deleteScrap(memberId, productId);
        return ResponseEntity.ok(success());
    }
}
