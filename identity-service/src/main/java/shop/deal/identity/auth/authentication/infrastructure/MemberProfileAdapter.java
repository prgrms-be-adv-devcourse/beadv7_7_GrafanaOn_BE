package shop.deal.identity.auth.authentication.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.deal.identity.auth.authentication.application.MemberProfilePort;
import shop.deal.identity.auth.authentication.application.dto.MemberProfileResult;
import shop.deal.identity.member.application.MemberService;
import shop.deal.identity.member.application.dto.CreateProfileCommand;
import shop.deal.identity.member.application.dto.MemberInfo;

// Member 코드가 shop.deal.identity.
@Component
@RequiredArgsConstructor
public class MemberProfileAdapter implements MemberProfilePort {
    private final MemberService memberService;

    @Override
    public MemberProfileResult createProfile(
            String name,
            String defaultShippingAddress,
            String phoneNumber
    ) {
        CreateProfileCommand command = new CreateProfileCommand(
                name,
                defaultShippingAddress,
                phoneNumber
        );
        MemberInfo memberInfo = memberService.createProfile(command);

        return new MemberProfileResult(memberInfo.id(), memberInfo.nickname());
    }
}
