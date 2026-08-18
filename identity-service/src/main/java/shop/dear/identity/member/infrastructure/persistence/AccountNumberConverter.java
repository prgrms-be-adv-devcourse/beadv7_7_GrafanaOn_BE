package shop.dear.identity.member.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.port.CipherManager;
import shop.dear.identity.member.domain.model.AccountNumber;

/**
 * 계좌번호를 영속화할 때 암호화한다.
 *
 * <p>{@code autoApply = true} 이므로 {@link AccountNumber} 타입 속성에 자동 적용된다.
 * 덕분에 도메인 모델이 {@code @Convert} 로 이 클래스를 참조하지 않아도 되고,
 * 의존 방향이 infrastructure → domain 으로 유지된다.
 *
 * <p>주의: 전용 타입이 아닌 {@code String} 을 대상으로 자동 적용하면
 * 모든 문자열 컬럼이 암호화되므로 반드시 {@link AccountNumber} 를 대상으로 유지해야 한다.
 */
@Converter(autoApply = true)
@Component
@RequiredArgsConstructor
public class AccountNumberConverter implements AttributeConverter<AccountNumber, String> {

	private final CipherManager cipherManager;

	@Override
	public String convertToDatabaseColumn(final AccountNumber attribute) {

		if (attribute == null) {
			return null;
		}

		return cipherManager.encode(attribute.value());
	}

	@Override
	public AccountNumber convertToEntityAttribute(final String dbData) {

		if (dbData == null) {
			return null;
		}

		return AccountNumber.restore(cipherManager.decode(dbData));
	}
}
