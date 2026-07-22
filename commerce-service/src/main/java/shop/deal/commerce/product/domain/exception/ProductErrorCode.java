package shop.deal.commerce.product.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    INVALID_SELLER("PD-001", "판매자가 아닌 사용자는 판매 상품을 등록할 수 없습니다."),
    INVALID_PRODUCT_IMAGE_TYPE("PD-002", "지원하지 않는 이미지 확장자입니다."),
    ;

    private final String value;
    private final String message;
}
