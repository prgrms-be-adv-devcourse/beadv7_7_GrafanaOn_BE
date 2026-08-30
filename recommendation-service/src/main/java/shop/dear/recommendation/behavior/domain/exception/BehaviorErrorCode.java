package shop.dear.recommendation.behavior.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum BehaviorErrorCode implements ErrorCode {

    RECOMMENDATION_ID_REQUIRED("RB-001", "VIEW/CLICK 이벤트에는 recommendationId가 필요합니다."),
    ;

    private final String value;
    private final String message;
}
