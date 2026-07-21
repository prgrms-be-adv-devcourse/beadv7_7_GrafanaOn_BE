package shop.deal.commerce.financial.wallet.domain;

public enum WalletLogType {
    PAYMENT,      // 결제
    SETTLEMENT,   // 정산
    TOPUP,        // 충전
    HOLD,         // 홀드
    RELEASE,      // 홀드 해제
}
