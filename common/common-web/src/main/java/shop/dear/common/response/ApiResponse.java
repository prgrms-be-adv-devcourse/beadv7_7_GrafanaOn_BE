package shop.dear.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "success";
    private static final String SUCCESS_MESSAGE = "API 요청이 정상적으로 수행되었습니다.";

    private String code;

    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> successWithData(final T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> fail(final ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getValue(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(final ErrorCode errorCode, final T data) {
        return new ApiResponse<>(errorCode.getValue(), errorCode.getMessage(), data);
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
