package shop.dear.commerce.financial.wallet.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum WalletErrorCode implements ErrorCode {
    WALLET_NOT_FOUND("WA-001", "존재하지 않는 지갑입니다."),
    INSUFFICIENT_BALANCE("WA-002", "잔액이 부족합니다."),
    INVALID_AMOUNT("WA-003", "유효하지 않은 금액입니다."),
    INVALID_REFERENCE_ID("WA-004", "유효하지 않은 참조 ID입니다."),
    INSUFFICIENT_HELD_BALANCE("WA-005", "hold된 금액이 부족합니다."),
    DUPLICATE_RELEASE("WA-006", "이미 처리된 release 요청입니다."),
    ;

    private final String value;
    private final String message;
}