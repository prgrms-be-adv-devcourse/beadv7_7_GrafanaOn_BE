package shop.dear.commerce.cart.infrastructure.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor
@Getter
public enum CartHttpErrorCode implements ErrorCode {
    EXTERNAL_API_ERROR("CA-004", "외부 시스템 연동 중 오류가 발생했습니다."),
    ;

    private final String value;
    private final String message;
}
