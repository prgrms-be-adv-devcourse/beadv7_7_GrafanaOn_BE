package shop.deal.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum CommonErrorCode implements ErrorCode {
    INTERNAL_SERVER_APPLICATION_ERROR("IA-001", "서버 애플리케이션에 예기치 못한 문제가 발생했습니다."),
    ;

    private final String value;
    private final String message;
}

