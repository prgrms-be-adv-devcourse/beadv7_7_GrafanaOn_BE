package shop.dear.commerce.order.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OrderErrorCode implements ErrorCode {
    FINANCIAL_SERVICE_UNAVAILABLE("OR-001", "금융 서비스와 통신할 수 없습니다.");

    private final String value;
    private final String message;
}
