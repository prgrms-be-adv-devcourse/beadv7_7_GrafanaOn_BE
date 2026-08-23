package shop.dear.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST_PARAMETER("CM-001", "요청 파라미터가 올바르지 않습니다."),
    INTERNAL_SERVER_APPLICATION_ERROR("IA-001", "서버 애플리케이션에 예기치 못한 문제가 발생했습니다."),
    AUTHENTICATION_REQUIRED("AT-001", "인증이 필요합니다."),
    ACCESS_DENIED("AT-002", "접근 권한이 없습니다."),
    INVALID_REQUEST("RQ-001", "입력값이 올바르지 않습니다."), // HTTP Request 오류
    ;

    private final String value;
    private final String message;
}

