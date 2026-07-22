package shop.deal.commerce.financial.wallet.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum WalletErrorCode implements ErrorCode {
    WALLET_NOT_FOUND("WA-001", "존재하지 않는 지갑입니다."),
    INSUFFICIENT_BALANCE("WA-002", "잔액이 부족합니다."),
    ;

    private final String value;
    private final String message;
}