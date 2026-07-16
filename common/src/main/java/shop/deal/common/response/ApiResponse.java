package shop.deal.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "success";
    private static final String SUCCESS_MESSAGE = "API 요청이 정상적으로 수행되었습니다.";

    private final String code;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> successWithData(final T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> fail(final ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getValue(), errorCode.getMessage(), null);
    }

    private ApiResponse(
        final String code,
        final String message,
        final T data
    ) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
