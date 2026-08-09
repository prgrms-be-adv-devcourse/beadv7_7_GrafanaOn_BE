package shop.dear.commerce.order.offer.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OfferErrorCode implements ErrorCode {
    INVALID_OFFER_STATUS_TRANSITION("OF-001", "현재 오퍼 상태에서는 수행할 수 없는 작업입니다."),
    INVALID_OFFER_PAYMENT_STATUS_TRANSITION("OF-002", "현재 오퍼 결제 상태에서는 수행할 수 없는 작업입니다."),
    OFFER_NOT_FOUND("OF-003", "해당 오퍼를 찾을 수 없습니다."),
    NOT_OFFER_SELLER("OF-004", "해당 오퍼에 대한 권한이 없습니다."),
    OFFER_PRICE_MISMATCH("OF-005", "오퍼 작성 시점의 상품 가격과 현재 가격이 일치하지 않습니다."),
    OFFER_MEMBER_NOT_FOUND("OF-006", "회원을 찾을 수 없습니다."),
    OFFER_PRODUCT_NOT_FOUND("OF-007", "상품을 찾을 수 없습니다."),
    OFFER_PRODUCT_LOOKUP_FAILED("OF-008", "상품 정보를 조회하지 못했습니다."),
    INVALID_OFFER_STATUS("OF-009", "유효하지 않은 오퍼 상태입니다."),
    ;

    private final String value;
    private final String message;
}
