package shop.dear.recommendation.domain.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.common.response.ErrorCode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum RecommendationErrorCode implements ErrorCode {

	INVALID_PAYLOAD_FORMAT("RC-001", "이벤트 형식이 올바르지 않습니다."),
	PRODUCT_ID_REQUIRED("RC-002", "상품 ID 가 필요합니다."),
	;

	private final String value;
	private final String message;
}
