package shop.dear.commerce.product.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    SELLER_ROLE_REQUIRED("PD-001", "판매자가 아닌 사용자는 판매 상품을 등록할 수 없습니다."),
    INVALID_PRODUCT_IMAGE_TYPE("PD-002", "지원하지 않는 이미지 확장자입니다."),
    PRODUCT_IMAGE_LIMIT_EXCEEDED("PD-003", "상품 사진은 최대 5장까지만 가능합니다."),
    ;

    private final String value;
    private final String message;
}
