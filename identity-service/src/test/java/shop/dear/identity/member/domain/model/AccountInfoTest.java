package shop.dear.identity.member.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountInfoTest {

    @Test
    @DisplayName("은행명과 계좌번호가 유효하면 생성에 성공한다")
    void createSuccess() {

        AccountInfo accountInfo = AccountInfo.of("국민은행", "1234567890");

        assertEquals("국민은행", accountInfo.getBank());
        assertEquals("1234567890", accountInfo.getAccount().value());
    }

    @ParameterizedTest
    @DisplayName("계좌번호가 10~16자리면 생성에 성공한다")
    @ValueSource(strings = {
        "1234567890",         // 최소 10자리
        "12345678901",
        "123456789012345",
        "1234567890123456"    // 최대 16자리
    })
    void acceptsAccountWithinAllowedLength(final String account) {

        AccountInfo accountInfo = AccountInfo.of("국민은행", account);

        assertEquals(account, accountInfo.getAccount().value());
    }

    @ParameterizedTest
    @DisplayName("계좌번호가 10자리 미만이거나 16자리를 넘으면 실패한다")
    @ValueSource(strings = {
        "1",
        "123456789",          // 9자리
        "12345678901234567"   // 17자리
    })
    void rejectsAccountOutOfAllowedLength(final String account) {

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> AccountInfo.of("국민은행", account));

        assertEquals(MemberErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    @ParameterizedTest
    @DisplayName("계좌번호에 숫자가 아닌 문자가 섞이면 실패한다")
    @ValueSource(strings = {
        "123-456-789",        // 하이픈은 허용하지 않는다
        "12345678a0",
        "123 456 7890",
        "１２３４５６７８９０"    // 전각 숫자
    })
    void rejectsNonNumericAccount(final String account) {

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> AccountInfo.of("국민은행", account));

        assertEquals(MemberErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    @ParameterizedTest
    @DisplayName("계좌번호가 없거나 비어 있으면 실패한다")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankAccount(final String account) {

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> AccountInfo.of("국민은행", account));

        assertEquals(MemberErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    @ParameterizedTest
    @DisplayName("은행명이 없거나 비어 있으면 실패한다")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankBank(final String bank) {

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> AccountInfo.of(bank, "1234567890"));

        assertEquals(MemberErrorCode.INVALID_BANK_CODE, exception.getErrorCode());
    }

    @Test
    @DisplayName("은행명과 계좌번호가 모두 같으면 동등하다")
    void equalsBySameBankAndAccount() {

        AccountInfo one = AccountInfo.of("국민은행", "1234567890");
        AccountInfo other = AccountInfo.of("국민은행", "1234567890");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("계좌번호가 다르면 동등하지 않다")
    void notEqualsWhenAccountDiffers() {

        AccountInfo one = AccountInfo.of("국민은행", "1234567890");
        AccountInfo other = AccountInfo.of("국민은행", "9876543210");

        Assertions.assertNotEquals(one, other);
    }
}
