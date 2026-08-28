package shop.dear.commerce.financial.payment.application.dto;

import java.math.BigDecimal;

public record PayOrderCommand(
        Long memberId,
        Long orderId,
        String orderType,
        BigDecimal amount
) {
}
