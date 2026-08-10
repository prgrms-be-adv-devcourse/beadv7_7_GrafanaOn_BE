package shop.dear.identity.member.infrastructure.persistence;

import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.port.CipherManager;

import java.util.List;

@Component
public class CipherContext implements CipherManager {

	private final List<CipherStrategy> strategies;
	private final AesGcmCipher currentStrategy;

	public CipherContext(
		final List<CipherStrategy> strategies,
		final AesGcmCipher currentStrategy
	) {
		this.strategies = strategies;
		this.currentStrategy = currentStrategy;
	}

	@Override
	public String encode(final String raw) {
		return currentStrategy.encode(raw);
	}

	@Override
	public String decode(final String encoded) {
		return strategies.stream()
			.filter(strategy -> strategy.supports(encoded))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("지원하지 않는 암호화 방식입니다."))
			.decode(encoded);
	}
}
