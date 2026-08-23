package shop.dear.identity.auth.authentication.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;
import shop.dear.common.exception.ServiceUnavailableException;
import shop.dear.common.response.ApiResponse;
import shop.dear.common.response.ErrorCode;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;

import java.util.List;

import static shop.dear.common.response.ApiResponse.fail;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "%s: %s".formatted(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();
        log.warn("Auth 요청값 검증 실패. errors = {}", errors);

        return ResponseEntity.badRequest()
                .body(fail(CommonErrorCode.INVALID_REQUEST, errors));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceUnavailableException(
            ServiceUnavailableException exception
    ) {
        log.warn("{} 발생! errorCode = {}",
                exception.getClass().getSimpleName(),
                exception.getErrorCode().getValue(),
                exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(fail(exception.getErrorCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = resolveStatus(errorCode);

        log.warn(
                "Auth 요청 실패. errorCode = {}",
                errorCode.getValue()
        );

        return ResponseEntity.status(status)
                .body(fail(errorCode));
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        if (errorCode == CommonErrorCode.AUTHENTICATION_REQUIRED) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (!(errorCode instanceof AuthErrorCode authErrorCode)) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (authErrorCode) {

            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;

            case INACTIVE_ACCOUNT -> HttpStatus.FORBIDDEN;

            case INVALID_CREDENTIALS,
                 AUTH_ACCOUNT_NOT_FOUND,
                 INVALID_TOKEN,
                 EXPIRED_TOKEN,
                 INVALID_REFRESH_TOKEN,
                 REFRESH_TOKEN_REUSE_DETECTED,
                 UNVERIFIED_SOCIAL_EMAIL -> HttpStatus.UNAUTHORIZED;
        };
    }
}
