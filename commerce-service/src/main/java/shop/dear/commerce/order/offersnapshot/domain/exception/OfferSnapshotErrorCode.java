package shop.dear.commerce.order.offersnapshot.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OfferSnapshotErrorCode implements ErrorCode {
    OFFER_SNAPSHOT_ALREADY_LINKED("OFS-001", "이미 오퍼에 연결된 스냅샷입니다."),
    ;

    private final String value;
    private final String message;
}
