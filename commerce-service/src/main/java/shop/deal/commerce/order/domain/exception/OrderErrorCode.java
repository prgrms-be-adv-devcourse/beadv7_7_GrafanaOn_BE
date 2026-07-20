package shop.deal.commerce.order.domain.exception;

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
    INVALID_OFFER_STATUS_TRANSITION("OD-004", "현재 오퍼 상태에서는 수행할 수 없는 작업입니다."),
    INVALID_OFFER_PAYMENT_STATUS_TRANSITION("OD-005", "현재 오퍼 결제 상태에서는 수행할 수 없는 작업입니다."),
    OFFER_SNAPSHOT_ALREADY_LINKED("OD-006", "이미 오퍼에 연결된 스냅샷입니다."),
    ;

    private final String value;
    private final String message;
}
