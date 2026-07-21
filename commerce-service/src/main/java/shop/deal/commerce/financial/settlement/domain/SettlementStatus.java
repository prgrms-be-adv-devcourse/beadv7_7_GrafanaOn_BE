package shop.deal.commerce.financial.settlement.domain;

public enum SettlementStatus {
    PENDING,   // 정산 대기 중
    COMPLETED,   // 정산 완료
    FAILED,   // 정산 실패
}
