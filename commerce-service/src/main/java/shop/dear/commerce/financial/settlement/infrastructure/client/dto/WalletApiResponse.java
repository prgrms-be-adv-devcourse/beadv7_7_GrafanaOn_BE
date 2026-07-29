package shop.dear.commerce.financial.settlement.infrastructure.client.dto;

public record WalletApiResponse(
	Long walletId,
	Long memberId
) {
}
