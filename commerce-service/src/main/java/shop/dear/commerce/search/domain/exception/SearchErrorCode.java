package shop.dear.commerce.search.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchErrorCode implements ErrorCode {
    REINDEX_ALREADY_RUNNING("SR-001", "재색인이 이미 실행 중입니다."),
    SEARCH_TEMPORARILY_UNAVAILABLE("SR-002", "검색이 일시적으로 제한되고 있습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final String value;
    private final String message;
}
