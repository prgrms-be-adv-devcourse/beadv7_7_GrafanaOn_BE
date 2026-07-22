package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.MemberInfo;

public record CreateProfileResponse(
    Long memberId
) {

    public static CreateProfileResponse from(MemberInfo info) {
        return new CreateProfileResponse(info.id());
    }
}
