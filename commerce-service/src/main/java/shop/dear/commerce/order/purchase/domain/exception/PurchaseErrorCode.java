package shop.dear.commerce.order.purchase.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum PurchaseErrorCode implements ErrorCode {
    PURCHASE_NOT_FOUND("PC-001", "구매 정보를 찾을 수 없습니다."),
    INVALID_PURCHASE_STATUS_TRANSITION("PC-002", "현재 구매 상태에서는 수행할 수 없는 작업입니다."),
    PURCHASE_CANNOT_BE_CANCELLED("PC-003", "취소할 수 없는 구매 상태입니다."),
    PURCHASE_CANNOT_BE_REFUNDED("PC-004", "환불할 수 없는 구매 상태입니다."),
    PRODUCT_NOT_FOUND("PC-005", "상품을 찾을 수 없습니다."),
    PRODUCT_LOOKUP_FAILED("PC-006", "상품 정보를 조회하지 못했습니다."),
    PRODUCT_NOT_ON_SALE("PC-007", "현재 판매 중인 상품이 아닙니다."),
    PRODUCT_NOT_FOR_IMMEDIATE_PURCHASE("PC-008", "즉시구매할 수 없는 상품입니다."),
    CANNOT_PURCHASE_OWN_PRODUCT("PC-009", "본인이 판매하는 상품은 구매할 수 없습니다."),
    INVALID_PRODUCT_RESPONSE("PC-010", "상품 정보가 올바르지 않습니다."),
    MEMBER_NOT_FOUND("PC-011", "회원을 찾을 수 없습니다."),
    PRODUCT_ALREADY_TRADING("PC-012", "현재 거래 중인 상품입니다."),
    ;

    private final String value;
    private final String message;
}
