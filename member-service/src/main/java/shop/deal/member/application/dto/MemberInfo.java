package shop.deal.member.application.dto;

import shop.deal.member.domain.model.Member;

public record MemberInfo(
    Long id,
    String nickname,
    String email
){
    public static MemberInfo from(Member member){
        return new MemberInfo(
            member.getId(),
            member.getNickname(),
            member.getEmail()
        );
    }
}
