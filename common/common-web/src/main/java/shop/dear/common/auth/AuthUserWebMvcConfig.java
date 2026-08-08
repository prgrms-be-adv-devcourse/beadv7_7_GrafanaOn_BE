package shop.dear.common.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// Spring MVC가 Controller 메서드의 파라미터를 확인한다.
// @AuthUser Long memberId를 확인함.
@Configuration
@RequiredArgsConstructor
public class AuthUserWebMvcConfig implements WebMvcConfigurer {
    private final AuthUserArgumentResolver authUserArgumentResolver;

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> resolvers
    ) {
        resolvers.add(authUserArgumentResolver);
    }
}
