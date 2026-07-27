package shop.dear.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String MEMBER_ID_HEADER =
            "X-Authenticated-Member-Id";


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAuthUser =
                parameter.hasParameterAnnotation(AuthUser.class);

        boolean isLongType =
                Long.class.equals(parameter.getParameterType());

        return hasAuthUser && isLongType;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        String memberIdHeader = webRequest.getHeader(MEMBER_ID_HEADER);

        if (!StringUtils.hasText(memberIdHeader)) {
            throw authenticationRequired();
        }

        try {
            long memberId = Long.parseLong(memberIdHeader);

            if (memberId <= 0) {
                throw authenticationRequired();
            }

            return memberId;
        } catch (NumberFormatException exception) {
            throw authenticationRequired();
        }
    }

    private BusinessException authenticationRequired() {
        return new BusinessException(
                CommonErrorCode.AUTHENTICATION_REQUIRED
        );
    }
}
