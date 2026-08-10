package shop.dear.identity.member.infrastructure.persistence;

import java.util.Arrays;

public enum CipherVersion {

	// v1 = 마커 없음 (AES/CBC, 마커 도입 이전)
	GCM("v2:");

	private final String prefix;

	CipherVersion(final String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	public boolean matches(final String encoded) {
		return encoded.startsWith(prefix);
	}

	//마커가 없으면 기존 암호화방식 (AES/CBC)
	public static boolean hasNoPrefix(final String encoded) {
		return Arrays.stream(values())
			.noneMatch(version -> version.matches(encoded));
	}
}
