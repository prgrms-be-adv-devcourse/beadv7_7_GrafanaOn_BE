package shop.dear.identity.member.presentation;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

import java.util.List;
import java.util.stream.Collectors;

import static shop.dear.common.exception.CommonErrorCode.INTERNAL_SERVER_APPLICATION_ERROR;
import static shop.dear.common.response.ApiResponse.fail;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {MemberController.class, InternalMemberController.class})
public class MemberExceptionHandler{

    private static final String NICKNAME_CONSTRAINT = "uk_member_nickname";

    @ExceptionHandler
    public ResponseEntity<ApiResponse<List<String>>> handleValidException(final MethodArgumentNotValidException e) {
        //잘못 입력된 필드 리스트 반환
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> "%s: %s (입력값: %s)"
                .formatted(
                    error.getField(),
                    error.getDefaultMessage(),
                    error.getRejectedValue()
                ))
            .collect(Collectors.toList());

        log.warn("{} 발생! errors={}", e.getClass().getSimpleName(), errors, e);

        return ResponseEntity.badRequest()
            .body(fail(MemberErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
        final DataIntegrityViolationException e) {

        if (isNicknameDuplicated(e)) {
            log.warn("닉네임 중복 발생", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(fail(MemberErrorCode.DUPLICATE_NICKNAME));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(fail(INTERNAL_SERVER_APPLICATION_ERROR));
    }

    private boolean isNicknameDuplicated(final DataIntegrityViolationException e) {

        return e.getCause() instanceof ConstraintViolationException hibernateException
            && NICKNAME_CONSTRAINT.equals(hibernateException.getConstraintName());
    }
}
