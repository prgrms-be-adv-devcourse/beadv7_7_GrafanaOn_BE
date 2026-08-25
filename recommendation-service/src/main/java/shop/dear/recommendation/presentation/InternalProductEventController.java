package shop.dear.recommendation.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import shop.dear.common.response.ApiResponse;
import shop.dear.recommendation.infrastructure.inbox.InboxService;
import shop.dear.recommendation.presentation.dto.request.ProductEventRelayRequest;

import java.util.List;

import static shop.dear.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/recommendation")
public class InternalProductEventController {

    private final InboxService inboxService;

    @PostMapping("/product-events")
    public ResponseEntity<ApiResponse<Void>> receiveProductEvents(
        @RequestBody final List<@Valid ProductEventRelayRequest> requests
    ) {
        inboxService.saveProductEvents(
            requests.stream()
                .map(ProductEventRelayRequest::toInbox)
                .toList()
        );

        return ResponseEntity.ok(success());
    }
}
