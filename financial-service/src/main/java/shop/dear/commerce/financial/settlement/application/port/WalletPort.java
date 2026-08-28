package shop.dear.commerce.financial.settlement.application.port;

import shop.dear.commerce.financial.settlement.application.dto.WalletInfo;

public interface WalletPort {

	WalletInfo getWalletId(Long memberId);
}
