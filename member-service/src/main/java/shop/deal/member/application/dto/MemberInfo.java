package shop.deal.member.application.dto;

import shop.deal.member.domain.Member;

public record MemberInfo(
    Long id,
    String name,
    String email
){
    public static MemberInfo from(Member member){
        return null;
    }
}
