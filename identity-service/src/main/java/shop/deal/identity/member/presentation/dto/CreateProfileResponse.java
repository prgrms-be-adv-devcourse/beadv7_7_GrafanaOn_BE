package shop.deal.identity.member.presentation.dto;

import shop.deal.identity.member.application.dto.MemberInfo;

public record CreateProfileResponse(
    Long memberId
) {

    public static CreateProfileResponse from(MemberInfo info) {
        return new CreateProfileResponse(info.id());
    }
}
