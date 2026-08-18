package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.MemberInfo;

public record PublicProfileResponse(
	Long id,
	String nickname
) {
	public static PublicProfileResponse from(MemberInfo info) {

		return new PublicProfileResponse(
			info.id(),
			info.nickname()
		);
	}
}
