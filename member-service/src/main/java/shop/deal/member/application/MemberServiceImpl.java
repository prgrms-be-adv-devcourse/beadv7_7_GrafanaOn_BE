package shop.deal.member.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.application.dto.RegisterCommand;
import shop.deal.member.domain.Member;
import shop.deal.member.domain.MemberRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public MemberInfo register(final RegisterCommand command) {

        if(memberRepository.existsByEmail(command.email())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        String nickname = createDefaultNickname();
        String encodePassword = passwordEncoder.encode(command.password());

        Member member = Member.create(
                command.name(),
                command.email(),
                encodePassword,
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
