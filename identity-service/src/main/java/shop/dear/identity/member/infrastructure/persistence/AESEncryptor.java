package shop.dear.identity.member.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.Encryptor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AESEncryptor implements Encryptor {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivParamSpec;

    public AESEncryptor(
        @Value("${identity.security.account-key}") final String key,
        @Value("${identity.security.account-iv}") final String iv
    ) {
        this.keySpec = new SecretKeySpec(HexFormat.of().parseHex(key), "AES");
        this.ivParamSpec = new IvParameterSpec(HexFormat.of().parseHex(iv));
    }

    @Override
    public String encode(final String raw) {

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);

            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("계좌번호 암호화에 실패했습니다.", e);
        }
    }

    @Override
    public String decode(final String encoded) {

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encoded));

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                 | IllegalArgumentException e) {
            throw new IllegalStateException("계좌번호 복호화에 실패했습니다.", e);
        }
    }
}
