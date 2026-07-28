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
    ;

    private final String value;
    private final String message;
}
