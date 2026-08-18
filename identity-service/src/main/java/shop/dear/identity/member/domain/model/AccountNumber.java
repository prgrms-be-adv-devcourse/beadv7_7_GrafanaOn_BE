package shop.dear.identity.member.domain.model;

import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

public final class AccountNumber {

	private static final String FORMAT = "\\d{10,16}";

	private final String value;

	private AccountNumber(final String value) {
		this.value = value;
	}

	public static AccountNumber of(final String value) {

		if (value == null || !value.matches(FORMAT)) {
			throw new BusinessException(MemberErrorCode.INVALID_ACCOUNT_NUMBER);
		}

		return new AccountNumber(value);
	}

	public static AccountNumber restore(final String value) {
		return new AccountNumber(value);
	}

	public String value() {
		return value;
	}
}
