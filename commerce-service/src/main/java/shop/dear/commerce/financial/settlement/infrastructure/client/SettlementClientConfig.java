package shop.dear.commerce.financial.settlement.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SettlementClientConfig {

	@Bean
	RestClient settlementRestClient(@Value("${order.client.base-url}") String baseUrl) {
		return RestClient.builder()
			.baseUrl(baseUrl)
			.build();
	}
}
