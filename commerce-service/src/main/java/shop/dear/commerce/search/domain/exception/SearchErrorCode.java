package shop.dear.commerce.search.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchErrorCode implements ErrorCode {
    REINDEX_ALREADY_RUNNING("SR-001", "재색인이 이미 실행 중입니다."),
    ;

    private final String value;
    private final String message;
}
