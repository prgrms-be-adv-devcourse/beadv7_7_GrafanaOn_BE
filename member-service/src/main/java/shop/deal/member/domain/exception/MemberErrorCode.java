package shop.deal.member.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum MemberErrorCode implements ErrorCode {
    DUPLICATE_NICKNAME("MB-001", "이미 등록된 닉네임입니다."),
    INVALID_INPUT("MB-002", "입력값이 잘못되었습니다."),
    ;

    private final String value;
    private final String message;
}
