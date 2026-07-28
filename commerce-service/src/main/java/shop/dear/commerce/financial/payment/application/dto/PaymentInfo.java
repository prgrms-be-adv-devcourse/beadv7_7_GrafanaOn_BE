package shop.dear.commerce.financial.payment.application.dto;

import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.model.Payment;

public record PaymentInfo(
        Long paymentId,
        PaymentStatus state
) {
    public static PaymentInfo from(final Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getState()
        );
    }
}
