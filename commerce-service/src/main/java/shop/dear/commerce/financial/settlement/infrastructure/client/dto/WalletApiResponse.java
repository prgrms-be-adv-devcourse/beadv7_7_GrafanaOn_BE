package shop.dear.commerce.financial.settlement.infrastructure.client.dto;

import shop.dear.commerce.financial.wallet.domain.model.Wallet;

public record WalletApiResponse(
	Long walletId,
	Long memberId
) {
	public static WalletApiResponse from(final Wallet wallet) {
		return new WalletApiResponse(wallet.getId(), wallet.getMemberId());
	}
}
