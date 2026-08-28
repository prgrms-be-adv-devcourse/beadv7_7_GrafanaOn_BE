package shop.dear.commerce.financial.settlement.domain.model;

import java.math.BigDecimal;

public record SettlementSummary(
	Integer count,
	BigDecimal grossAmount,
	BigDecimal feeAmount,
	BigDecimal netAmount
) {
}
