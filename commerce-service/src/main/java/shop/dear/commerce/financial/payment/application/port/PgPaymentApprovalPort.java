package shop.dear.commerce.financial.payment.application.port;

import shop.dear.commerce.financial.payment.application.dto.PgApprovalResult;

import java.math.BigDecimal;

public interface PgPaymentApprovalPort {

    PgApprovalResult approve(
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String idempotencyKey
    );
}
