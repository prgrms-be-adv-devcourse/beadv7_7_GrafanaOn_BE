package shop.deal.member.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.deal.member.application.dto.CreateProfileCommand;
import shop.deal.member.domain.repository.MemberRepository;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
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
}
