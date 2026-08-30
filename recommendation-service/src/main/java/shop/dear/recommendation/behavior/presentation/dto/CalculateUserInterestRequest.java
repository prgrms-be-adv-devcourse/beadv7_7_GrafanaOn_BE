package shop.dear.recommendation.behavior.presentation.dto;

import java.time.LocalDateTime;

public record CalculateUserInterestRequest (
        Long memberId,
        LocalDateTime since
) {
}
