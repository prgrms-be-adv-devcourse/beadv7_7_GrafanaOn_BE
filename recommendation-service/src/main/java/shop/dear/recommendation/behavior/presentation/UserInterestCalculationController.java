package shop.dear.recommendation.behavior.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.common.response.ApiResponse;
import shop.dear.recommendation.behavior.application.UserInterestCalculationService;
import shop.dear.recommendation.behavior.presentation.dto.CalculateUserInterestRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations/userInterests")
public class UserInterestCalculationController {

    private final UserInterestCalculationService userInterestCalculationService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Void>> calculateUserInterests(
            @RequestBody final CalculateUserInterestRequest request
    ) {
        userInterestCalculationService.calculateUserInterests(
                request.memberId(),
                request.since()
        );
        return ResponseEntity.ok(ApiResponse.success());
    }
}
