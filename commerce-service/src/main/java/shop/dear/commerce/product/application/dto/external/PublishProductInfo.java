package shop.dear.commerce.product.application.dto.external;

import java.time.LocalDateTime;

public record PublishProductInfo(
    LocalDateTime startTime,
    LocalDateTime endTime,
    int count
) {
}
