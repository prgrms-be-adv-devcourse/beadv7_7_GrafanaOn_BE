package shop.deal.member.application;

import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.application.dto.SignUpCommand;

public interface MemberService {

    MemberInfo signUp(SignUpCommand command);
}
