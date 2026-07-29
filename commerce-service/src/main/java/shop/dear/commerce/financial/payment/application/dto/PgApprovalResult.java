package shop.dear.commerce.financial.payment.application.dto;

import java.math.BigDecimal;

public record PgApprovalResult(
        String orderId,
        String transactionKey,
        BigDecimal approvedAmount
) {
}
