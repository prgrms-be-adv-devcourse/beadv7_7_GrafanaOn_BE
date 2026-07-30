package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.identity.member.application.port.WalletPort;
import shop.dear.identity.member.infrastructure.client.dto.WalletApiRequest;

@Component
@RequiredArgsConstructor
public class WalletHttpClient implements WalletPort {

    private final RestClient walletRestClient;

    @Override
    public void createWallet(final Long memberId) {
        walletRestClient.post()
            .uri("/internal/deposits")
            .body(new WalletApiRequest(memberId))
            .retrieve()
            .toBodilessEntity();
    }
}
