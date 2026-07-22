package shop.deal.member.application.dto;

import shop.deal.member.domain.model.Member;

public record MemberInfo(
    Long id,
    String name,
    String defaultShippingAddress,
    String phoneNumber,
    String nickname
){
    public static MemberInfo from(Member member){
        return new MemberInfo(
            member.getId(),
            member.getName(),
            member.getDefaultShippingAddress(),
            member.getPhoneNumber(),
            member.getNickname()
        );
    }
}
