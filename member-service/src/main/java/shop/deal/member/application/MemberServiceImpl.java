package shop.deal.member.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.deal.common.exception.BusinessException;
import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.application.dto.SignUpCommand;
import shop.deal.member.domain.exception.MemberErrorCode;
import shop.deal.member.domain.model.Member;
import shop.deal.member.domain.repository.MemberRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public MemberInfo signUp(final SignUpCommand command) {

        if(memberRepository.existsByEmail(command.email())) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
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
