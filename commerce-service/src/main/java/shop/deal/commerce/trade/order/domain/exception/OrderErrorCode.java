package shop.deal.commerce.trade.order.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OrderErrorCode implements ErrorCode {
    INVALID_ORDER_STATUS_TRANSITION("OD-001", "현재 주문 상태에서는 수행할 수 없는 작업입니다."),
    ORDER_CANNOT_BE_CANCELLED("OD-002", "취소할 수 없는 주문 상태입니다."),
    ORDER_CANNOT_BE_REFUNDED("OD-003", "환불할 수 없는 주문 상태입니다."),
    ;

    private final String value;
    private final String message;
}
