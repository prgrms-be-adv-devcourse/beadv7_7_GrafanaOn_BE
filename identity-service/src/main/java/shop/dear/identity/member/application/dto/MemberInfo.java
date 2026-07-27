package shop.dear.identity.member.application.dto;

import shop.dear.identity.member.domain.constract.MemberStatus;
import shop.dear.identity.member.domain.model.Member;

public record MemberInfo(
    Long id,
    String name,
    String defaultShippingAddress,
    String phoneNumber,
    String nickname,
    MemberStatus status
){
    public static MemberInfo from(Member member){
        return new MemberInfo(
            member.getId(),
            member.getName(),
            member.getDefaultShippingAddress(),
            member.getPhoneNumber(),
            member.getNickname(),
            member.getStatus()
        );
    }
}
