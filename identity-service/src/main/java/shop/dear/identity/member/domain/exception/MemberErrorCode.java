package shop.dear.identity.member.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum MemberErrorCode implements ErrorCode {
    DUPLICATE_NICKNAME("MB-001", "이미 등록된 닉네임입니다."),
    INVALID_INPUT("MB-002", "입력값이 잘못되었습니다."),
    MEMBER_NOT_FOUND("MB-003", "존재하지 않는 회원ID입니다."),
    ALREADY_SELLER("MB-004", "이미 판매자로 등록된 회원입니다."),
    NOT_SELLER("MB-005", "판매자로 등록되지 않은 회원입니다."),
    WITHDRAWAL_FAILED("MB-006", "판매상품이 등록되어 있어 해지가 불가합니다."),
    ;

    private final String value;
    private final String message;
}
