package shop.dear.identity.member.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.port.CipherManager;

@Converter
@Component
@RequiredArgsConstructor
public class AccountConverter implements AttributeConverter<String, String> {

	private final CipherManager cipherManager;

	@Override
	public String convertToDatabaseColumn(final String attribute) {
		if (attribute == null) {
			return null;
		}
		return cipherManager.encode(attribute);
	}

	@Override
	public String convertToEntityAttribute(final String dbData) {
		if (dbData == null) {
			return null;
		}
		return cipherManager.decode(dbData);
	}
}
