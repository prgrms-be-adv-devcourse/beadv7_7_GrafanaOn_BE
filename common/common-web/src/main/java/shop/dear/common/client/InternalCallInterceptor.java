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

        String memberId = resolveMemberId();

        if (StringUtils.hasText(memberId)) {
            request.getHeaders().set(AuthUser.MEMBER_ID_HEADER, memberId);
        }

        request.getHeaders().remove(TRACE_ID_HEADER);
        String traceId = MDC.get("traceId");

        if (StringUtils.hasText(traceId)) {
            request.getHeaders().set(TRACE_ID_HEADER, traceId);
        }

        return execution.execute(request, body);
    }

    /**
     * 차단기를 거친 호출은 별도 스레드에서 실행되어 요청 컨텍스트가 없다.
     * 그때 스냅샷을 쓰고, 요청 스레드에서 직접 호출된 경우는 기존대로 request에서 쓴다.
     */
    private String resolveMemberId() {
        String snapshot = InternalCallContext.getMemberId();

        if (StringUtils.hasText(snapshot)) {
            return snapshot;
        }

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(AuthUser.MEMBER_ID_HEADER);
        }

        return null;
    }
}
