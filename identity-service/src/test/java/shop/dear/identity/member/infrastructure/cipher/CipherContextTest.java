package shop.dear.identity.member.infrastructure.cipher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CipherContextTest {

    private static final String KEY = "56cf2c4ba176bcf24474211dcfef0339a850a61c58c8533aa33cf025362c5d50";
    private static final String LEGACY_IV = "9a273be1934c638c2472dedc7176cdcd";
    private static final String ACCOUNT = "123-456-789";

    private final AesGcmCipher gcmCipher = new AesGcmCipher(KEY);
    private final AesCbcCipher legacyCipher = new AesCbcCipher(KEY, LEGACY_IV);
    private final CipherContext cipherContext =
        new CipherContext(List.of(gcmCipher, legacyCipher), gcmCipher);

    @Test
    @DisplayName("암호화한 계좌번호를 복호화하면 원본이 나온다")
    void encodeAndDecode() {

        String encoded = cipherContext.encode(ACCOUNT);

        assertNotEquals(ACCOUNT, encoded);
        assertEquals(ACCOUNT, cipherContext.decode(encoded));
    }

    @Test
    @DisplayName("암호문에는 포맷 구분을 위한 버전 프리픽스가 붙는다")
    void encodeAddsVersionPrefix() {

        assertTrue(cipherContext.encode(ACCOUNT).startsWith(CipherVersion.GCM.getPrefix()));
    }

    @Test
    @DisplayName("같은 계좌번호라도 IV가 매번 달라 암호문이 서로 다르다")
    void encodeProducesDifferentCipherTextForSameInput() {

        assertNotEquals(cipherContext.encode(ACCOUNT), cipherContext.encode(ACCOUNT));
    }

    @Test
    @DisplayName("CBC로 저장된 기존 계좌번호는 레거시 전략으로 복호화된다")
    void decodeSupportsLegacyCbcFormat() {

        assertEquals(ACCOUNT, cipherContext.decode(legacyCbcEncode(ACCOUNT)));
    }

    @Test
    @DisplayName("레거시 암호문은 첫 바이트값과 무관하게 GCM으로 오분류되지 않는다")
    void legacyCipherTextIsNeverMisroutedToGcm() {

        // 마커를 Base64 페이로드 안쪽 첫 바이트로 두면 1/256 확률로 오분류되던 케이스
        String collides = Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02, 0x03});

        assertTrue(legacyCipher.supports(collides));
        assertFalse(gcmCipher.supports(collides));

        for (int i = 0; i < 1000; i++) {
            assertFalse(gcmCipher.supports(legacyCbcEncode("account-" + i)));
        }
    }

    @Test
    @DisplayName("암호문이 변조되면 복호화에 실패한다")
    void decodeRejectsTamperedCipherText() {

        String encoded = cipherContext.encode(ACCOUNT);
        String tampered = encoded.substring(0, encoded.length() - 2)
            + (encoded.endsWith("A") ? "B=" : "A=");

        assertThrows(IllegalStateException.class, () -> cipherContext.decode(tampered));
    }

    @Test
    @DisplayName("형식이 잘못된 값은 IllegalStateException으로 감싸진다")
    void decodeWrapsMalformedInput() {

        String prefix = CipherVersion.GCM.getPrefix();

        assertThrows(IllegalStateException.class, () -> cipherContext.decode(prefix + "not-base64!!"));
        assertThrows(IllegalStateException.class,
            () -> cipherContext.decode(prefix + Base64.getEncoder().encodeToString(new byte[8])));
        assertThrows(IllegalStateException.class, () -> cipherContext.decode(prefix));
        assertThrows(IllegalStateException.class, () -> cipherContext.decode(""));
        // 탈퇴한 셀러의 계좌번호 자리에 저장되는 리터럴
        assertThrows(IllegalStateException.class, () -> cipherContext.decode("****"));
    }

    @Test
    @DisplayName("지원하는 전략이 없으면 복호화에 실패한다")
    void decodeFailsWhenNoStrategySupportsFormat() {

        CipherContext gcmOnly = new CipherContext(List.of(gcmCipher), gcmCipher);

        assertThrows(IllegalStateException.class, () -> gcmOnly.decode(legacyCbcEncode(ACCOUNT)));
    }

    /** 마이그레이션 이전 방식(AES/CBC/PKCS5Padding, 고정 IV)으로 암호화한 값을 재현한다. */
    private String legacyCbcEncode(final String raw) {

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(HexFormat.of().parseHex(KEY), "AES"),
                new IvParameterSpec(HexFormat.of().parseHex(LEGACY_IV)));

            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
