package shop.dear.commerce.order.offersnapshot.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum OfferSnapshotErrorCode implements ErrorCode {
    OFFER_SNAPSHOT_ALREADY_LINKED("OFS-001", "이미 오퍼에 연결된 스냅샷입니다."),
    OFFER_SNAPSHOT_NOT_FOUND("OFS-002", "오퍼 스냅샷을 찾을 수 없습니다."),
    OFFER_SNAPSHOT_WRITER_MISMATCH("OFS-003", "오퍼 스냅샷 작성자가 일치하지 않습니다."),
    ;

    private final String value;
    private final String message;
}
