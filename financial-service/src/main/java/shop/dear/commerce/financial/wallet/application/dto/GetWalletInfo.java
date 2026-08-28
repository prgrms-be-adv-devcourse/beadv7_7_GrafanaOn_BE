package shop.dear.commerce.financial.wallet.application.dto;

import shop.dear.commerce.financial.wallet.domain.model.Wallet;

public record GetWalletInfo(
	Long walletId,
	Long memberId
) {
	public static GetWalletInfo from(Wallet wallet) {
		return new GetWalletInfo(
			wallet.getId(),
			wallet.getMemberId()
		);
	}
}
