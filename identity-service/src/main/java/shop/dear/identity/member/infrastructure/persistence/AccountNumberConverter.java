package shop.dear.identity.member.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.port.CipherManager;
import shop.dear.identity.member.domain.model.AccountNumber;

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
