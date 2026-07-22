package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.MemberInfo;

public record MemberResponse(
    Long id,
    String name,
    String defaultShippingAddress,
    String phoneNumber,
    String nickname
) {

    public static MemberResponse from(MemberInfo info) {
        return new MemberResponse(
            info.id(),
            info.name(),
            info.defaultShippingAddress(),
            info.phoneNumber(),
            info.nickname()
        );
    }
}
