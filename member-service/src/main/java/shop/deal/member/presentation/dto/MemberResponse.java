package shop.deal.member.presentation.dto;

import shop.deal.member.application.dto.MemberInfo;

public record MemberResponse(
        Long id,
        String nickname,
        String email
) {

    public static MemberResponse from(MemberInfo info) {
        return new MemberResponse(info.id(), info.nickname(), info.email());
    }
}
