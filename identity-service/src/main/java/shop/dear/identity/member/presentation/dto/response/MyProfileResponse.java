package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.MemberInfo;

public record MyProfileResponse (
	Long id,
	String name,
	String defaultShippingAddress,
	String phoneNumber,
	String nickname
) {

	public static MyProfileResponse from(MemberInfo info) {

		return new MyProfileResponse(
			info.id(),
			info.name(),
			info.defaultShippingAddress(),
			info.phoneNumber(),
			info.nickname()
		);
	}
}
