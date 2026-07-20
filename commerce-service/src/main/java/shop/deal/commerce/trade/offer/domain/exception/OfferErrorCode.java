package shop.deal.commerce.trade.offer.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OfferErrorCode implements ErrorCode {
    INVALID_OFFER_STATUS_TRANSITION("OF-001", "현재 오퍼 상태에서는 수행할 수 없는 작업입니다."),
    INVALID_OFFER_PAYMENT_STATUS_TRANSITION("OF-002", "현재 오퍼 결제 상태에서는 수행할 수 없는 작업입니다."),
    OFFER_SNAPSHOT_ALREADY_LINKED("OF-003", "이미 오퍼에 연결된 스냅샷입니다."),
    ;

    private final String value;
    private final String message;
}
