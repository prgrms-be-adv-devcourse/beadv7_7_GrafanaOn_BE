package shop.dear.commerce.financial.settlement.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.financial.settlement.application.dto.WalletInfo;
import shop.dear.commerce.financial.settlement.application.port.WalletPort;
import shop.dear.commerce.financial.settlement.infrastructure.client.dto.WalletApiResponse;
import shop.dear.common.response.ApiResponse;

@Component
@RequiredArgsConstructor
public class WalletApiClient implements WalletPort {

	private final RestClient walletRestClient;

	@Override
	public WalletInfo getWalletId() {
		ApiResponse<WalletApiResponse> body = walletRestClient.get()
			.uri("/internal/deposits")
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		WalletApiResponse data = body.getData();

		return new WalletInfo(data.memberId(), data.walletId());
	}
}
