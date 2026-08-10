package shop.dear.identity.member.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AesCbcEncryptor implements CipherStrategy {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivParamSpec;

    public AesCbcEncryptor(
        @Value("${identity.security.account-key}") final String key,
        @Value("${identity.security.account-iv}") final String iv
    ) {
        this.keySpec = new SecretKeySpec(HexFormat.of().parseHex(key), "AES");
        this.ivParamSpec = new IvParameterSpec(HexFormat.of().parseHex(iv));
    }

    @Override
    public boolean supports(final String encoded) {
        // 버전 마커가 없는 값 = 마커 도입 이전에 CBC로 저장된 계좌번호
        return CipherVersion.hasNoPrefix(encoded);
    }

    @Override
    public String decode(final String encoded) {

        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encoded);

            // 빈 입력은 CBC 복호화가 예외 없이 빈 문자열을 돌려주므로 여기서 걸러낸다.
            if (cipherBytes.length == 0) {
                throw new GeneralSecurityException("암호문이 비어 있습니다.");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("계좌번호 복호화에 실패했습니다.", e);
        }
    }
}
