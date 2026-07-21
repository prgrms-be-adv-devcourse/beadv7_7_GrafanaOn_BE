package shop.deal.member.presentation.dto;

import shop.deal.member.application.dto.MemberInfo;

public record MemberResponse(
    Long id,
    String name,
    String email,
    String defaultShippingAddress,
    String phoneNumber,
    String nickname
) {

    public static MemberResponse from(MemberInfo info) {
        return new MemberResponse(
            info.id(),
            info.name(),
            info.email(),
            info.defaultShippingAddress(),
            info.phoneNumber(),
            info.nickname()
        );
    }
}
