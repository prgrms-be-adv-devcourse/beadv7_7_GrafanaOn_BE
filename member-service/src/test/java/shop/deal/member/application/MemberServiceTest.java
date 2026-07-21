package shop.deal.member.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.deal.member.application.dto.SignUpCommand;
import shop.deal.member.domain.repository.MemberRepository;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 성공")
    void signUpTest(){

        SignUpCommand command = new SignUpCommand(
            "테스트",
            "test@example.com",
            "abc12345!",
            "서울시 강남구",
            "010-1234-5678"
        );

        given(memberRepository
            .save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        memberService.signUp(command);

        verify(memberRepository).existsByEmail(command.email());
        verify(passwordEncoder).encode(command.rawPassword());
        verify(memberRepository).save(any());
    }
}
