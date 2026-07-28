package shop.dear.commerce.financial.settlementpolicy.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SettlementPolicyErrorCode implements ErrorCode {
    SETTLEMENT_POLICY_NOT_FOUND("SP-001", "존재하지 않는 정산 정책입니다."),
    ;

    private final String value;
    private final String message;
}
