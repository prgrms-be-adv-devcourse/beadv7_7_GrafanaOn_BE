package shop.dear.common.exception;

import shop.dear.common.response.ErrorCode;

/**
 * 협력 서비스가 일시적으로 응답하지 못할 때 던진다.
 * 400도 아니고 500도 아닌 잠시 후에는 될 것으로 기대하는 503 에러를 던진다.
 */
public class ServiceUnavailableException extends BusinessException {
    public ServiceUnavailableException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
