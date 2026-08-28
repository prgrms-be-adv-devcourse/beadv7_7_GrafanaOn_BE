package shop.dear.commerce.financial.wallet.application.event;

public record WalletDebitFailedEvent(
        Long paymentId
) {
}
