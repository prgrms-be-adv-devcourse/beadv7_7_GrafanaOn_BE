package shop.dear.commerce.financial.wallet.presentation.dto.response;

import shop.dear.commerce.financial.wallet.application.dto.WalletInfo;

import java.math.BigDecimal;

public record WalletResponse(
        BigDecimal availableBalance,
        BigDecimal heldBalance
) {
    public static WalletResponse from(final WalletInfo walletInfo) {
        return new WalletResponse(
                walletInfo.availableBalance(),
                walletInfo.heldBalance()
        );
    }
}
