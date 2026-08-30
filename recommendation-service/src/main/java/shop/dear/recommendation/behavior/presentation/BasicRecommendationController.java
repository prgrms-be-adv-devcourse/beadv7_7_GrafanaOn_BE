package shop.dear.recommendation.behavior.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.recommendation.behavior.application.BasicRecommendationService;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class BasicRecommendationController {

    private final BasicRecommendationService basicRecommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<BasicRecommendationResponse>> recommend(
            @AuthUser final Long memberId,
            @RequestParam(defaultValue = "10") final int limit
    ) {
        BasicRecommendationResponse response =
                basicRecommendationService.recommend(memberId, limit);

        return ResponseEntity.ok(ApiResponse.successWithData(response));
    }
}
