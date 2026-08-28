package shop.dear.commerce.financial.payment.application.dto;

import shop.dear.commerce.financial.payment.domain.model.Payment;

import java.math.BigDecimal;

public record ChargeInfo(
        Long paymentId,
        String orderId,
        BigDecimal amount,
        String orderName
) {
    public static ChargeInfo from(final Payment payment) {
        return new ChargeInfo(
                payment.getId(),
                payment.getPgPayment().getMerchantOrderId(),
                payment.getAmount(),
                "예치금 충전"
        );
    }
}
