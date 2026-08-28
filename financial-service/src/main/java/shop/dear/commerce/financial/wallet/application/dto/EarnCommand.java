package shop.dear.commerce.financial.wallet.application.dto;

import java.math.BigDecimal;

public record EarnCommand (
	Long walletId,
    BigDecimal amount,
	Long referenceId
){
}
