package shop.dear.common.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextPropagatingExecutorServiceTest {

    // 같은 작업 스레드가 재사용되는지 확인하기 위해 단일 스레드로 둔다.
    private final ExecutorService executor = new ContextPropagatingExecutorService(Executors.newSingleThreadExecutor());

    @AfterEach
    void tearDown() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
        executor.shutdown();
    }

    @Test
    @DisplayName("제출한 스레드의 MDC가 작업 스레드로 전달된다.")
    void propagatesMdc() throws Exception {
        MDC.put("traceId", "trace-1");

        final String traceId = executor.submit(() -> MDC.get("traceId")).get();

        assertThat(traceId).isEqualTo("trace-1");
    }

    @Test
    @DisplayName("제출한 스레드의 요청 헤더를 작업 스레드에서 읽을 수 있다.")
    void propagatesRequestAttributes() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Member-Id", "42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        final String memberId = executor.submit(() -> {
            final ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            return attributes == null
                    ? null
                    : attributes.getRequest().getHeader("X-Authenticated-Member-Id");
        }).get();

        assertThat(memberId).isEqualTo("42");
    }

    @Test
    @DisplayName("작업이 끝나면 memberId도 지워 다음 작업으로 새지 않는다")
    void clearsMemberIdAfterTask() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Member-Id", "42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        executor.submit(() -> InternalCallContext.getMemberId()).get();

        // 두 번째 작업은 컨텍스트 없이 제출한다. 같은 스레드가 재사용된다.
        RequestContextHolder.resetRequestAttributes();
        final String leaked = executor.submit(() -> InternalCallContext.getMemberId()).get();

        assertThat(leaked).isNull();
    }
}

