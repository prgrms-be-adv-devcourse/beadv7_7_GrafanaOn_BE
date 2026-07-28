package shop.dear.commerce.financial.wallet.presentation.dto.response;

import shop.dear.commerce.financial.wallet.application.dto.GetWalletInfo;

public record GetWalletResponse(
	Long walletId,
	Long memberId
) {
	public static GetWalletResponse from(final GetWalletInfo getWalletInfo) {
		return new GetWalletResponse(
			getWalletInfo.walletId(),
			getWalletInfo.memberId()
		);
	}
}
