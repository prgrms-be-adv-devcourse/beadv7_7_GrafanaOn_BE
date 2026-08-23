package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.identity.member.application.port.AuthRolePort;

@Component
@RequiredArgsConstructor
public class AuthRoleHttpClient implements AuthRolePort {

    private final RestClient authRestClient;

    // memberId 는 InternalCallInterceptor 가 현재 요청의 X-Authenticated-Member-Id 헤더를
    // 그대로 전파해주므로, 이 요청의 대상도 항상 같은 memberId 로 해석된다 (@AuthUser).
    @Override
    public void promoteToSeller(final Long memberId) {
        authRestClient.post()
                .uri("/internal/auth/role")
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void demoteToBuyer(final Long memberId) {
        authRestClient.delete()
                .uri("/internal/auth/role")
                .retrieve()
                .toBodilessEntity();
    }
}
