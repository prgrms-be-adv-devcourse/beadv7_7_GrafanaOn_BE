package shop.deal.member.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum MemberErrorCode implements ErrorCode {
    DUPLICATE_EMAIL("MB-001", "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME("MB-002", "이미 등록된 닉네임입니다."),
    INVALID_INPUT_VALUE("MB-003", "요청 값이 올바르지 않습니다."),
    ;

    private final String value;
    private final String message;
}
