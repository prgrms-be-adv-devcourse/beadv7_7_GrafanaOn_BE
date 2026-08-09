package shop.dear.commerce.financial.settlement.application.dto;

import shop.dear.commerce.financial.settlement.domain.model.Settlement;

import java.math.BigDecimal;

public record SettlementInfo(
	Long id,
	Long settlementPolicyId,
	Long walletId,
	Long purchaseId,
	Long offerId,
	BigDecimal grossAmount,
	BigDecimal feeAmount,
	BigDecimal netAmount
) {
	public static SettlementInfo from(Settlement settlement) {
		return new SettlementInfo(
			settlement.getId(),
			settlement.getSettlementPolicyId(),
			settlement.getWalletId(),
			settlement.getPurchaseId(),
			settlement.getOfferId(),
			settlement.getGrossAmount(),
			settlement.getFeeAmount(),
			settlement.getNetAmount()
		);
	}
}
