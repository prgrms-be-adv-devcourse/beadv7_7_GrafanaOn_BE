package shop.dear.commerce.financial.payment.domain.constant;

public enum PGPaymentStatus {
    // 토스 결제 처리 상태 응답값 매핑
    READY,
    IN_PROGRESS,
    WAITING_FOR_DEPOSIT,
    DONE,
    CANCELED,
    PARTIAL_CANCELED,
    ABORTED,
    EXPIRED
}
