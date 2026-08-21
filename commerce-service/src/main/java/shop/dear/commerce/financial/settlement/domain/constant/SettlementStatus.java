package shop.dear.commerce.financial.settlement.domain.constant;

public enum SettlementStatus {
    CREATED,     // 생성됨 (집계 전)
    PENDING,     // 집계에 누적됨, 지급 대기
    COMPLETED,   // 지급 완료
    FAILED,      // 정산 실패
}
