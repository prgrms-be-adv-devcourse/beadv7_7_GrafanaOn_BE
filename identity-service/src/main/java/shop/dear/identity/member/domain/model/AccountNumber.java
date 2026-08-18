package shop.dear.identity.member.domain.model;

import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

import java.util.Objects;

/**
 * 계좌번호 값 객체.
 *
 * <p>{@link AccountInfo} 의 한 필드이며 은행명과 함께 하나의 계좌정보를 이룬다.
 * 영속화 시 암호화되지만 그 방법은 인프라(AccountNumberConverter)가 정하므로
 * 이 클래스는 프레임워크에 의존하지 않는다.
 */
public final class AccountNumber {

	private static final String FORMAT = "\\d{10,16}";

	private final String value;

	private AccountNumber(final String value) {
		this.value = value;
	}

	/**
	 * 사용자 입력으로부터 생성한다. 형식을 검증한다.
	 */
	public static AccountNumber of(final String value) {

		if (value == null || !value.matches(FORMAT)) {
			throw new BusinessException(MemberErrorCode.INVALID_ACCOUNT_NUMBER);
		}

		return new AccountNumber(value);
	}

	/**
	 * 저장된 값으로부터 복원한다. 영속화 계층에서만 사용한다.
	 *
	 * <p>과거 규칙으로 저장된 값이 있을 수 있으므로 검증하지 않는다.
	 * 검증 규칙을 바꿨을 때 기존 데이터를 읽지 못하게 되는 것을 막기 위함이다.
	 */
	public static AccountNumber restore(final String value) {
		return new AccountNumber(value);
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}
		if (!(o instanceof AccountNumber other)) {
			return false;
		}

		return Objects.equals(this.value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	/**
	 * 계좌번호가 로그나 예외 메시지로 새어나가지 않도록 값을 노출하지 않는다.
	 */
	@Override
	public String toString() {
		return "AccountNumber(****)";
	}
}
