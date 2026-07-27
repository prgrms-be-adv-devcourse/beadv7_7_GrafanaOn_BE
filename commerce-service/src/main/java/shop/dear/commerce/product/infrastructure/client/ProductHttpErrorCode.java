package shop.dear.commerce.product.infrastructure.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProductHttpErrorCode implements ErrorCode {
    EXTERNAL_API_ERROR("PH-001", "외부 시스템 연동 중 오류가 발생했습니다."),
    ;

    private final String value;
    private final String message;
}
