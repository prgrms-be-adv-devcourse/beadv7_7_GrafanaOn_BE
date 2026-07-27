package shop.dear.identity.scrap.presentation;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.scrap.application.ScrapService;
import shop.dear.identity.scrap.application.dto.ScrapDetail;
import shop.dear.identity.scrap.presentation.dto.ScrapPageResponse;
import shop.dear.identity.scrap.presentation.dto.ScrapResponse;

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
    public ResponseEntity<ApiResponse<ScrapPageResponse>> getScrapList(
        @AuthUser final Long memberId,
        @RequestParam(defaultValue = "0") final int page,
        @RequestParam(defaultValue = "10") final int size
    ) {

        Page<ScrapDetail> scraps = scrapService.getScrapList(
            memberId,
            page,
            size
        );

        return ResponseEntity.ok(successWithData(ScrapPageResponse.from(scraps)));
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
