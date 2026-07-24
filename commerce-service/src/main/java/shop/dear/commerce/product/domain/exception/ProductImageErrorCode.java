package shop.dear.commerce.product.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProductImageErrorCode implements ErrorCode {
    INVALID_UPLOAD_FILE_TYPE("PI-001", "지원하지 않는 이미지 확장자입니다."),
    INVALID_URL("PI-002", "URL로 null 혹은 공백을 입력할 수 없습니다."),
    EXCEEDED_URL_LENGTH_LIMIT("PI-003", "URL이 최대 글자수를 초과했습니다."),
    INVALID_SORT_ORDER("PI-004", "유효하지 않은 이미지 정렬 순서입니다."),
    ;

    private final String value;
    private final String message;
}
