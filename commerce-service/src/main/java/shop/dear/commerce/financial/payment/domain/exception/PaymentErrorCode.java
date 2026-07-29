package shop.dear.commerce.financial.payment.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND("PM-001", "존재하지 않는 결제입니다."),
    INVALID_PAYMENT_STATUS_TRANSITION("PM-002", "현재 결제 상태에서는 수행할 수 없는 작업입니다."),
    INVALID_AMOUNT("PM-003", "유효하지 않은 금액입니다."),
    INVALID_ORDER_REFERENCE("PM-004", "유효하지 않은 주문 참조입니다."),
    INVALID_MEMBER_ID("PM-005", "유효하지 않은 회원 ID입니다."),
    INVALID_PAYMENT_PURPOSE("PM-006", "유효하지 않은 결제 목적입니다."),
    PG_PAYMENT_ALREADY_PREPARED("PM-007", "이미 준비된 PG 결제입니다."),
    PAYMENT_ACCESS_DENIED("PM-008", "해당 결제에 접근할 권한이 없습니다."),
    PG_APPROVAL_FAILED("PM-009", "PG 결제 승인에 실패했습니다."),
    PAYMENT_CONFIRMATION_MISMATCH("PM-010", "결제 승인 정보가 저장된 결제 정보와 일치하지 않습니다."
    );

    private final String value;
    private final String message;
}
