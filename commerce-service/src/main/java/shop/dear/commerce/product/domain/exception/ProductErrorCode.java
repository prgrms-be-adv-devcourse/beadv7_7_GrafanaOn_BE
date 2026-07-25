package shop.dear.commerce.product.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    REQUIRED_SELLER_ROLE("PD-001", "판매자가 아닌 사용자는 판매 상품을 등록할 수 없습니다."),
    INVALID_NAME("PD-002", "상품 이름으로 null 혹은 공백을 입력할 수 없습니다."),
    EXCEEDED_NAME_LENGTH_LIMIT("PD-003", "상품 이름이 최대 글자수를 초과했습니다."),
    INVALID_BRAND("PD-004", "상품 브랜드로 null 혹은 공백을 입력할 수 없습니다."),
    EXCEEDED_BRAND_LENGTH_LIMIT("PD-005", "상품 브랜드가 최대 글자수를 초과했습니다."),
    INVALID_MODEL_NUMBER("PD-006", "상품 모델번호로 null 혹은 공백을 입력할 수 없습니다."),
    EXCEEDED_MODEL_NUMBER_LENGTH_LIMIT("PD-007", "상품 모델번호가 최대 글자수를 초과했습니다."),
    INVALID_PRODUCT_CATEGORY("PD-008", "상품 카테고리로 null을 입력할 수 없습니다."),
    INVALID_RELEASE_DATE("PD-009", "발매일은 미래 날짜로 설정할 수 없습니다."),
    INVALID_PRODUCT_SALE_TYPE("PD-010", "상품 판매방식으로 null을 입력할 수 없습니다."),
    EXCEEDED_DESCRIPTION_LENGTH_LIMIT("PD-011", "상품 상세설명이 최대 글자수를 초과했습니다."),
    EXCEEDED_PRODUCT_IMAGE_COUNT_LIMIT("PD-012", "상품 이미지 등록 가능 개수를 초과했습니다."),
    ALREADY_EXISTS_SORT_ORDER_NUMBER("PD-013", "이미 동일한 순서의 상품 이미지가 존재합니다."),
    INVALID_PRODUCT("PD-014", "존재하지 않는 상품입니다."),
    NOT_PRODUCT_SELLER("PD-015", "해당 상품의 판매자가 아닙니다."),
    INVALID_PRODUCT_STATUS_FOR_UPDATE("PD-016", "해당 상품을 수정할 수 있는 상태가 아닙니다."),
    INVALID_PRODUCT_PRICE("PD-017", "상품 가격으로 null을 입력할 수 없습니다."),
    PRODUCT_PRICE_CANNOT_BE_NEGATIVE("PD-018", "상품 가격은 음수일 수 없습니다."),
    INVALID_PRODUCT_STATUS_FOR_DELETE("PD-019", "해당 상품을 삭제할 수 있는 상태가 아닙니다."),
    ;

    private final String value;
    private final String message;
}
