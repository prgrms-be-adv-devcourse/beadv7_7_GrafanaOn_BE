package shop.dear.commerce.financial.settlement.presentation.dto.response;

import shop.dear.commerce.financial.settlement.application.dto.SettlementInfo;

import java.math.BigDecimal;
import java.util.List;

public record SettlementResponse(
	Long id,
	Long settlementPolicyId,
	Long walletId,
	Long purchaseId,
	Long offerId,
	BigDecimal grossAmount,
	BigDecimal feeAmount,
	BigDecimal netAmount
) {
	public static SettlementResponse from(SettlementInfo info) {
		return new SettlementResponse(
			info.id(),
			info.settlementPolicyId(),
			info.walletId(),
			info.purchaseId(),
			info.offerId(),
			info.grossAmount(),
			info.feeAmount(),
			info.netAmount()
		);
	}
}
