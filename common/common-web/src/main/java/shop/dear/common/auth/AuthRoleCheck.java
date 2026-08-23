package shop.dear.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;

import java.util.Arrays;

@Aspect
@Component
public class AuthRoleCheck {

    @Before("@annotation(AuthRole)")
    public void checkRole(final AuthRole AuthRole) {

        final String role = currentRole();

        if (!StringUtils.hasText(role) || !Arrays.asList(AuthRole.roles()).contains(role)) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }
    }

    private String currentRole() {
        final ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
        }

        final HttpServletRequest request = attributes.getRequest();

        return request.getHeader(AuthRole.ROLE_HEADER);
    }
}
