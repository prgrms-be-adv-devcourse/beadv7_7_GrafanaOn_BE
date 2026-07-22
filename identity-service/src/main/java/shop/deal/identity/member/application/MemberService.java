package shop.deal.identity.member.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.deal.common.exception.BusinessException;
import shop.deal.identity.member.application.dto.CreateProfileCommand;
import shop.deal.identity.member.application.dto.MemberInfo;
import shop.deal.identity.member.domain.exception.MemberErrorCode;
import shop.deal.identity.member.domain.model.Member;
import shop.deal.identity.member.domain.repository.MemberRepository;

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

    public MemberInfo getProfile(final Long memberId){

        return memberRepository.findById(memberId)
            .map(MemberInfo::from)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
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
