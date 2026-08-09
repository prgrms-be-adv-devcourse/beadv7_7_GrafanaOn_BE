package shop.dear.commerce.financial.payment.application.dto;

import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;

public record PayOrderCommand(
        Long memberId,
        Long orderId,
        OrderType orderType,
        BigDecimal amount
) {
}
