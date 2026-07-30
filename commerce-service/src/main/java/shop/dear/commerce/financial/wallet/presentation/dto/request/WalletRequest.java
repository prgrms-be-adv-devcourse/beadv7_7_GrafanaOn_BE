package shop.dear.commerce.financial.wallet.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record WalletRequest(
        @NotNull Long memberId
) {
}
