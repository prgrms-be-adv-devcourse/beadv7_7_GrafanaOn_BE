package shop.dear.commerce.cart.domain.exception;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum CartErrorCode implements ErrorCode {
    CART_ITEM_NOT_FOUND("CT-001", "존재하지 않는 장바구니 상품입니다."),
    MIN_QUANTITY_REQUIRED("CT-002", "수량은 1개 이상이어야 합니다."),
    CART_NOT_FOUND("CT-003", "장바구니를 찾을 수 없습니다."),
    INVALID_DELETE_REQUEST("CT-004", "삭제할 상품을 선택해주세요."),
    PRODUCT_NOT_FOUND("CT-005", "상품을 찾을 수 없습니다."),
    PRODUCT_LOOKUP_FAILED("CT-006", "상품 정보 조회에 실패했습니다."),
    INVALID_PRODUCT_TYPE("CT-007", "상품 타입이 맞지않습니다")
    ;

    private final String value;
    private final String message;
}
