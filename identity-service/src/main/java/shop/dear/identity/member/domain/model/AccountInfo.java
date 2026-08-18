package shop.dear.identity.member.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.infrastructure.persistence.AccountConverter;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInfo {

	@Column(name = "bank", nullable = false)
	private String bank;

	@Convert(converter = AccountConverter.class)
	@Column(name = "account", nullable = false)
	private String account;

	private AccountInfo(final String bank, final String account) {
		this.bank = bank;
		this.account = account;
	}

	public static AccountInfo of(final String bank, final String account) {
		validate(bank, account);
		return new AccountInfo(bank, account);
	}

	private static void validate(final String bank, final String account) {
		if (bank == null || bank.isBlank()) {
			throw new BusinessException(MemberErrorCode.INVALID_BANK_CODE);
		}

		if (account == null || !account.matches("\\d{10,16}")) {
			throw new BusinessException(MemberErrorCode.INVALID_ACCOUNT_NUMBER);
		}
	}
}