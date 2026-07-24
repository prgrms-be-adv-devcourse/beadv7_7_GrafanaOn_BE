package shop.dear.identity.scrap.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.scrap.application.ScrapService;
import shop.dear.identity.scrap.presentation.dto.ScrapResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/{productId}")
    public ApiResponse<ScrapResponse> addScrap(
        @PathVariable final Long productId,
        @RequestHeader("X-Member-Id") final Long memberId
    ) {

        ScrapResponse scrap = ScrapResponse.from(scrapService.addScrap(memberId, productId));

        return ApiResponse.successWithData(scrap);
    }

    @GetMapping
    public ApiResponse<List<ScrapResponse>> getScrapList(@RequestHeader("X-Member-Id") final Long memberId) {

        List<ScrapResponse> scraps = scrapService.getScrapList(memberId).stream()
            .map(ScrapResponse::from)
            .toList();

        return ApiResponse.successWithData(scraps);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteScrap(
        @PathVariable final Long productId,
        @RequestHeader("X-Member-Id") final Long memberId
    ) {

        scrapService.deleteScrap(memberId, productId);

        return ApiResponse.success();
    }
}
