package shop.dear.common.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import shop.dear.common.auth.AuthUser;

import java.io.IOException;

@Component
public class InternalCallInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().remove(AuthUser.MEMBER_ID_HEADER);

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String memberId = attributes.getRequest()
                    .getHeader(AuthUser.MEMBER_ID_HEADER);

            if (StringUtils.hasText(memberId)) {
                request.getHeaders().set(AuthUser.MEMBER_ID_HEADER, memberId);
            }
        }

        return execution.execute(request, body);
    }
}
