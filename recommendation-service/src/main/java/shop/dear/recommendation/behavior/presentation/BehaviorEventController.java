package shop.dear.recommendation.behavior.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static shop.dear.common.response.ApiResponse.success;
import shop.dear.common.response.ApiResponse;
import shop.dear.recommendation.behavior.application.BehaviorEventService;
import shop.dear.recommendation.behavior.presentation.dto.TrackBehaviorRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/behaviors")
public class BehaviorEventController {

    private final BehaviorEventService behaviorEventService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> trackBehaviorEvent(
            @RequestBody final TrackBehaviorRequest request
    ) {
        behaviorEventService.track(request.toCommand());
        return ResponseEntity.ok(success());
    }
}
