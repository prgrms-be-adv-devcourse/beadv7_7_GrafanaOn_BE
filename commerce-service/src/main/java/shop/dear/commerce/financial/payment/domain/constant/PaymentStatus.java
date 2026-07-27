package shop.dear.commerce.financial.payment.domain.constant;

public enum PaymentStatus {
    PENDING,       // 결제 요청 진행 중
    PAID,          // 결제 완료
    FAILED,        // 결제 실패
    CANCELLED,     // 결제 취소
}
