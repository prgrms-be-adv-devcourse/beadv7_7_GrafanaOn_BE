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
    ;

    private final String value;
    private final String message;
}
