package shop.dear.commerce.financial.payment.presentation.dto.response;

import shop.dear.commerce.financial.payment.application.dto.ChargeInfo;

import java.math.BigDecimal;

public record ChargeResponse(
        Long paymentId,
        String orderId,
        BigDecimal amount,
        String orderName
) {
    public static ChargeResponse from(final ChargeInfo info) {
        return new ChargeResponse(
                info.paymentId(),
                info.orderId(),
                info.amount(),
                info.orderName()
        );
    }
}
