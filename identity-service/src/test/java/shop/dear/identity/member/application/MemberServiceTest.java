package shop.dear.identity.member.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.dto.CreateProfileCommand;
import shop.dear.identity.member.application.dto.UpdateProfileCommand;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.repository.MemberRepository;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("프로필생성 성공")
    void createProfileTest(){

        CreateProfileCommand command = new CreateProfileCommand(
            "테스트",
            "서울시 강남구",
            "010-1234-5678"
        );

        given(memberRepository
            .save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        memberService.createProfile(command);

        verify(memberRepository).save(any());
    }

    @Test
    @DisplayName("프로필 조회")
    void getProfileTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        given(memberRepository
            .findById(any()))
            .willReturn(Optional.of(member));

        memberService.getProfile(1L);

        verify(memberRepository).findById(any());
    }

    @Test
    @DisplayName("프로필 수정")
    void updateProfileTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        UpdateProfileCommand command = new UpdateProfileCommand(
            "부산시 해운대구",
            "010-9999-9999",
            "user_000001");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.updateProfile(command, 1L);

        Assertions.assertEquals("테스트", member.getName());
        Assertions.assertEquals("부산시 해운대구", member.getDefaultShippingAddress());
        Assertions.assertEquals("010-9999-9999", member.getPhoneNumber());
        verify(memberRepository, never()).existsByNickname(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("프로필 수정 (닉네임 중복)")
    void updateProfile_duplicateNickname(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "다람쥐");

        UpdateProfileCommand command = new UpdateProfileCommand(
            "서울시 강남구",
            "010-1234-5678",
            "너구리");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("너구리")).willReturn(true);

        Assertions.assertThrows(BusinessException.class, () -> memberService.updateProfile(command, 1L));
    }
}
