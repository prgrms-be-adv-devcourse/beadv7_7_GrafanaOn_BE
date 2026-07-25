package shop.dear.identity.scrap.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum ScrapErrorCode implements ErrorCode {
    SCRAP_NOT_FOUND("SC-001", "존재하지 않는 스크랩입니다."),
    DUPLICATE_SCRAP("SC-002", "이미 스크랩한 상품입니다."),
    ;

    private final String value;
    private final String message;
}
