package shop.dear.identity.member.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AesGcmEncryptor implements CipherStrategy {

	private static final CipherVersion VERSION = CipherVersion.GCM;

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int IV_LENGTH_BYTE = 12;
	private static final int TAG_LENGTH_BIT = 128;

	private final SecretKeySpec keySpec;
	private final SecureRandom secureRandom = new SecureRandom();

	public AesGcmEncryptor(
		@Value("${identity.security.account-key}") final String key
	) {
		this.keySpec = new SecretKeySpec(HexFormat.of().parseHex(key), "AES");
	}

	@Override
	public boolean supports(final String encoded) {
		return VERSION.matches(encoded);
	}

	public String encode(final String raw) {

		byte[] iv = new byte[IV_LENGTH_BYTE];
		secureRandom.nextBytes(iv);

		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			cipher.updateAAD(versionAad());

			byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

			// 복호화 시 필요하므로 IV를 암호문 앞에 붙여 함께 저장한다.
			byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
				.put(iv)
				.put(encrypted)
				.array();

			return VERSION.getPrefix() + Base64.getEncoder().encodeToString(combined);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("계좌번호 암호화에 실패했습니다.", e);
		}
	}

	@Override
	public String decode(final String encoded) {

		if (!supports(encoded)) {
			throw new IllegalStateException("계좌번호 복호화에 실패했습니다.");
		}

		try {
			byte[] combined = Base64.getDecoder()
				.decode(encoded.substring(VERSION.getPrefix().length()));

			byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTE);
			byte[] cipherBytes = Arrays.copyOfRange(combined, IV_LENGTH_BYTE, combined.length);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			cipher.updateAAD(versionAad());

			return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			// Base64 형식 오류(IllegalArgumentException)도 함께 감싼다.
			throw new IllegalStateException("계좌번호 복호화에 실패했습니다.", e);
		}
	}

	private byte[] versionAad() {
		return VERSION.getPrefix().getBytes(StandardCharsets.US_ASCII);
	}
}
