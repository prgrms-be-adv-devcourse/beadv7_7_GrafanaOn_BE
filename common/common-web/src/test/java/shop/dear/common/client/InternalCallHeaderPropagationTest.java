package shop.dear.common.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import shop.dear.common.auth.AuthUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 실제로 나가는 요청에 헤더가 붙는지 확인한다.
 *
 * 인터셉터 테스트는 헤더 맵만 보고, 실행기 테스트는 ThreadLocal만 본다.
 * 둘을 이어 붙인 경로는 어디서도 검증되지 않으므로 RestClient까지 태워서 확인한다.
 */
class InternalCallHeaderPropagationTest {

    private static final String BASE_URL = "http://internal-service";
    private static final String PATH = "/internal/ping";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // 차단기가 실제 호출을 돌리는 스레드와 같은 역할을 한다.
    private final ExecutorService executor =
            new ContextPropagatingExecutorService(Executors.newSingleThreadExecutor());

    private RestClient restClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder =
                new InternalRestClientFactory(new InternalCallInterceptor()).builder(BASE_URL);

        // bindTo가 요청 팩토리를 가짜로 갈아끼운다. 인터셉터는 그대로 살아 있다.
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.restClient = builder.build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
        executor.shutdown();
    }

    @Test
    @DisplayName("요청 스레드에서 직접 호출하면 요청 헤더의 memberId가 그대로 나간다")
    void sendsMemberIdFromRequestThread() {
        givenIncomingRequest("42");

        server.expect(requestTo(BASE_URL + PATH))
                .andExpect(header(AuthUser.MEMBER_ID_HEADER, "42"))
                .andRespond(withSuccess());

        restClient.get().uri(PATH).retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    @DisplayName("차단기 스레드에서 호출해도 스냅샷의 memberId와 traceId가 나간다")
    void sendsMemberIdFromCircuitBreakerThread() throws Exception {
        givenIncomingRequest("42");
        MDC.put("traceId", "trace-1");

        server.expect(requestTo(BASE_URL + PATH))
                .andExpect(header(AuthUser.MEMBER_ID_HEADER, "42"))
                .andExpect(header(TRACE_ID_HEADER, "trace-1"))
                .andRespond(withSuccess());

        // 차단기가 하는 일과 같다. 요청 컨텍스트가 없는 다른 스레드에서 호출이 일어난다.
        executor.submit(() -> restClient.get().uri(PATH).retrieve().toBodilessEntity()).get();

        server.verify();
    }

    @Test
    @DisplayName("요청 컨텍스트도 스냅샷도 없으면 memberId 헤더를 붙이지 않는다")
    void omitsMemberIdWithoutContext() throws Exception {
        server.expect(requestTo(BASE_URL + PATH))
                .andExpect(headerDoesNotExist(AuthUser.MEMBER_ID_HEADER))
                .andRespond(withSuccess());

        executor.submit(() -> restClient.get().uri(PATH).retrieve().toBodilessEntity()).get();

        server.verify();
    }

    private void givenIncomingRequest(final String memberId) {
        final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(AuthUser.MEMBER_ID_HEADER, memberId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
    }
}