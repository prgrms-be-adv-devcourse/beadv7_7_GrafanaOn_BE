package shop.deal.member.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.deal.member.application.dto.CreateProfileCommand;
import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.domain.model.Member;
import shop.deal.member.domain.repository.MemberRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MemberService  {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberInfo createProfile(final CreateProfileCommand command) {

        String nickname = createDefaultNickname();

        Member member = Member.create(
            command.name(),
            command.defaultShippingAddress(),
            command.phoneNumber(),
            nickname);

        return MemberInfo.from(memberRepository.save(member));
    }

    private String createDefaultNickname() {

        String nickname;

        do {
            int random = ThreadLocalRandom.current().nextInt(1_000_000);
            nickname = "user_" + String.format("%06d", random);
        } while (memberRepository.existsByNickname(nickname));

        return nickname;
    }
}
