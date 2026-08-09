package shop.dear.commerce.financial.settlement.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import shop.dear.common.client.InternalRestClientFactory;

@Configuration
@RequiredArgsConstructor
public class SettlementClientConfig {
	private final InternalRestClientFactory internalRestClientFactory;

	@Bean
	RestClient walletRestClient(@Value("${wallet.client.base-url}") String baseUrl) {

		return internalRestClientFactory.builder(baseUrl).build();
	}
}
