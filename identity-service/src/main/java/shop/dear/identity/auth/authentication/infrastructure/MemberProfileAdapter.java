package shop.dear.identity.auth.authentication.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.identity.auth.authentication.application.port.MemberProfilePort;
import shop.dear.identity.auth.authentication.application.dto.MemberProfileResult;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.application.dto.CreateProfileCommand;
import shop.dear.identity.member.application.dto.MemberInfo;


// Member 코드가 shop.dear.identity.
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
