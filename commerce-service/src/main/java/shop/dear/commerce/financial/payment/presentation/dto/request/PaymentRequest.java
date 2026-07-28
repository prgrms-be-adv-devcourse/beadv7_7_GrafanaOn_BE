package shop.dear.commerce.financial.payment.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.common.event.order.OrderType;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
        @Positive
        Long orderId,

        @NotNull
        OrderType orderType,

        @NotNull
        @Positive
        BigDecimal amount
) {
    public PayOrderCommand toCommand(final Long memberId) {
        return new PayOrderCommand(
                memberId,
                orderId,
                orderType,
                amount
        );
    }
}
