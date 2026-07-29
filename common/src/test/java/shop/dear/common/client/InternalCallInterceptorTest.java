package shop.dear.common.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import shop.dear.common.auth.AuthUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class InternalCallInterceptorTest {

    private final InternalCallInterceptor interceptor =
            new InternalCallInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void relayAuthenticatedMemberId() throws Exception {
        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest();

        servletRequest.addHeader(
                AuthUser.MEMBER_ID_HEADER,
                "1"
        );

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(servletRequest)
        );

        HttpHeaders outgoingHeaders = new HttpHeaders();
        outgoingHeaders.set(AuthUser.MEMBER_ID_HEADER, "999");

        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse =
                mock(ClientHttpResponse.class);

        byte[] body = new byte[0];

        given(request.getHeaders()).willReturn(outgoingHeaders);
        given(execution.execute(request, body))
                .willReturn(expectedResponse);

        ClientHttpResponse result =
                interceptor.intercept(request, body, execution);

        assertThat(
                outgoingHeaders.getFirst(AuthUser.MEMBER_ID_HEADER)
        ).isEqualTo("1");

        assertThat(result).isSameAs(expectedResponse);
    }

    @Test
    void removeMemberIdWhenRequestContextDoesNotExist() throws Exception {
        HttpHeaders outgoingHeaders = new HttpHeaders();
        outgoingHeaders.set(AuthUser.MEMBER_ID_HEADER, "999");

        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse =
                mock(ClientHttpResponse.class);

        byte[] body = new byte[0];

        given(request.getHeaders()).willReturn(outgoingHeaders);
        given(execution.execute(request, body))
                .willReturn(expectedResponse);

        interceptor.intercept(request, body, execution);

        assertThat(
                outgoingHeaders.getFirst(AuthUser.MEMBER_ID_HEADER)
        ).isNull();
    }

    @Test
    void removeMemberIdWhenIncomingHeaderIsBlank() throws Exception {
        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest();

        servletRequest.addHeader(
                AuthUser.MEMBER_ID_HEADER,
                " "
        );

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(servletRequest)
        );

        HttpHeaders outgoingHeaders = new HttpHeaders();
        outgoingHeaders.set(AuthUser.MEMBER_ID_HEADER, "999");

        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);

        byte[] body = new byte[0];

        given(request.getHeaders()).willReturn(outgoingHeaders);

        interceptor.intercept(request, body, execution);

        assertThat(
                outgoingHeaders.getFirst(AuthUser.MEMBER_ID_HEADER)
        ).isNull();
    }
}