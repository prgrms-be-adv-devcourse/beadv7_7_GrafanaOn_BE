package shop.dear.commerce.financial.settlement.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SettlementErrorCode implements ErrorCode {
    SETTLEMENT_NOT_FOUND("ST-001", "존재하지 않는 정산입니다."),
    INVALID_SETTLEMENT_STATUS_TRANSITION("ST-002", "현재 정산 상태에서는 수행할 수 없는 작업입니다."),
    INVALID_SETTLEMENT_BATCH_STATUS_TRANSITION("ST-003", "현재 정산 집계 상태에서는 수행할 수 없는 작업입니다."),
    SETTLEMENT_BATCH_WALLET_MISMATCH("ST-004", "정산 집계와 정산 건의 지갑이 일치하지 않습니다."),
    SETTLEMENT_BATCH_NOT_FOUND("ST-005", "존재하지 않는 정산 집계입니다."),
    ;

    private final String value;
    private final String message;
}
