package shop.dear.identity.member.presentation;

import lombok.extern.slf4j.Slf4j;
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

import static shop.dear.common.response.ApiResponse.fail;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {MemberController.class, InternalMemberController.class})
public class MemberExceptionHandler{

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
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {

        log.warn("{} 발생! 닉네임 중복", e.getClass().getSimpleName(), e);

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(fail(MemberErrorCode.DUPLICATE_NICKNAME));
    }

}
