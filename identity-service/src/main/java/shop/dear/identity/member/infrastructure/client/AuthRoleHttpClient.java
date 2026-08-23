package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.identity.member.application.port.AuthRolePort;

@Component
@RequiredArgsConstructor
public class AuthRoleHttpClient implements AuthRolePort {

    private final RestClient authRestClient;

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
