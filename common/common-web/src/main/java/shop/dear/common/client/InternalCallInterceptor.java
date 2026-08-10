package shop.dear.common.client;

import org.slf4j.MDC;
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

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

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

        request.getHeaders().remove(TRACE_ID_HEADER);
        String traceId = MDC.get("traceId");

        if (StringUtils.hasText(traceId)) {
            request.getHeaders().set(TRACE_ID_HEADER, traceId);
        }

        return execution.execute(request, body);
    }
}
