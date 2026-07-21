package shop.deal.member.application;

import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.application.dto.RegisterCommand;

public interface MemberService {

    MemberInfo register(RegisterCommand command);
}
