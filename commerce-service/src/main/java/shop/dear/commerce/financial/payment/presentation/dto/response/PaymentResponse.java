package shop.dear.commerce.financial.payment.presentation.dto.response;

import shop.dear.commerce.financial.payment.application.dto.PaymentInfo;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;

public record PaymentResponse(
        Long paymentId,
        PaymentStatus status
) {
    public static PaymentResponse from(final PaymentInfo paymentInfo) {
        return new PaymentResponse(
                paymentInfo.paymentId(),
                paymentInfo.state()
        );
    }
}
