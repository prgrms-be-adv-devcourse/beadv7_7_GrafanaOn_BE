package shop.dear.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.exception.CommonErrorCode.INTERNAL_SERVER_APPLICATION_ERROR;
import static shop.dear.common.exception.CommonErrorCode.INVALID_REQUEST_PARAMETER;
import static shop.dear.common.response.ApiResponse.fail;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(final NoResourceFoundException e) {
        log.warn("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.notFound()
            .build();
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException e) {
        log.warn("{} 발생! errorCode={}", e.getClass().getSimpleName(), e.getErrorCode().getValue(), e);
        return ResponseEntity.badRequest()
            .body(fail(e.getErrorCode()));
    }

    @ExceptionHandler

    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(final ConstraintViolationException e) {
        log.warn("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.badRequest()
                .body(fail(INVALID_REQUEST_PARAMETER));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception e) {
        log.error("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.internalServerError()
            .body(fail(INTERNAL_SERVER_APPLICATION_ERROR));
    }
}
