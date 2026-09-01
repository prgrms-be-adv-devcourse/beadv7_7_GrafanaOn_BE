package shop.dear.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라우트 차단기가 열렸거나 하위 서비스 호출이 실패했을 때 대신 응답한다.
 * 원래 요청의 메서드를 그대로 물고 넘어오므로 메서드를 제한하지 않는다.
 * 만약 제한한다면 다른 요청이 405를 받게 된다.
 */
@Slf4j
@RestController
public class FallbackController {

    private static final String ERROR_CODE = "GW-001";

    @RequestMapping("/fallback/{routeId}")
    public ResponseEntity<FallbackResponse> fallback(@PathVariable final String routeId) {
        log.warn("라우트 fallback 응답. routeId = {}", routeId);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FallbackResponse(ERROR_CODE, messageOf(routeId)));
    }

    private String messageOf(final String routeId) {
        return switch(routeId) {
            case "identity-auth" -> "로그인 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.";
            case "commerce-search" -> "검색 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.";
            case "financial-api" -> "결제 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.";
            case "recommendation-api" -> "추천 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.";
            default -> "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.";
        };
    }
}
