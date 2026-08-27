package shop.dear.recommendation.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.common.response.ApiResponse;
import shop.dear.recommendation.application.RecommendationService;
import shop.dear.recommendation.presentation.dto.response.RecommendSimilarItemsResponse;

import java.util.List;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

	private final RecommendationService recommendationService;

	@GetMapping("/similar")
	public ResponseEntity<ApiResponse<List<RecommendSimilarItemsResponse>>> recommendSimilarItems(
		@RequestParam(required = false) final Long productId,
		@RequestParam(required = false) final Integer size
	) {
		final List<RecommendSimilarItemsResponse> responses = RecommendSimilarItemsResponse.listOf(
			this.recommendationService.findSimilarItems(productId, size)
		);

		return ResponseEntity.ok(successWithData(responses));
	}
}
