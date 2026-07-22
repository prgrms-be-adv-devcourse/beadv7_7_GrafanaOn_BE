package shop.dear.identity.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.dto.CreateProfileCommand;
import shop.dear.identity.member.application.dto.MemberInfo;
import shop.dear.identity.member.application.dto.UpdateProfileCommand;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.repository.MemberRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    @Transactional
    public MemberInfo updateProfile(final UpdateProfileCommand command, final Long memberId){

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        boolean nicknameChanged = !member.getNickname().equals(command.nickname());
        if(nicknameChanged && memberRepository.existsByNickname(command.nickname())){
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        try {
            member.changeProfile(
                command.defaultShippingAddress(),
                command.phoneNumber(),
                command.nickname());
        } catch ( DataIntegrityViolationException e){
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        return MemberInfo.from(member);
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
