package shop.dear.identity.auth.authentication.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_EMAIL("AU-001", "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS("AU-002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_ACCOUNT_NOT_FOUND("AU-003", "인증 계정을 찾을 수 없습니다."),
    INACTIVE_ACCOUNT("AU-004", "로그인할 수 없는 계정입니다."),
    INVALID_TOKEN("AU-005", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("AU-006", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN("AU-007", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_REUSE_DETECTED("AU-008", "Refresh Token 재사용이 감지되었습니다. 다시 로그인해 주세요."),
    UNVERIFIED_SOCIAL_EMAIL("AU-009", "이메일이 확인되지 않은 소셜 계정입니다."),
            ;

    private final String value;
    private final String message;
}
