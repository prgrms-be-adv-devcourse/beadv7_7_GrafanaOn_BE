package shop.dear.identity.member.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInfo {

	@Column(name = "bank", nullable = false)
	private String bank;

	// 암호화는 AccountNumberConverter 가 autoApply 로 처리한다.
	@Column(name = "account", nullable = false)
	private AccountNumber account;

	private AccountInfo(final String bank, final AccountNumber account) {
		this.bank = bank;
		this.account = account;
	}

	public static AccountInfo of(final String bank, final String account) {
		validateBank(bank);
		return new AccountInfo(bank, AccountNumber.of(account));
	}

	private static void validateBank(final String bank) {
		if (bank == null || bank.isBlank()) {
			throw new BusinessException(MemberErrorCode.INVALID_BANK_CODE);
		}
	}
}