package shop.dear.common.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 헤더 전파와 연결 2초 / 읽기 3초는 공통에서 관리하고
 * 도메인별 응답, 예외 처리만 각 ClientConfig에 남긴다.
 */
@Component
@RequiredArgsConstructor
public class InternalRestClientFactory {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);
    private final InternalCallInterceptor internalCallInterceptor;

    public RestClient.Builder builder(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(internalCallInterceptor);
    }
}
