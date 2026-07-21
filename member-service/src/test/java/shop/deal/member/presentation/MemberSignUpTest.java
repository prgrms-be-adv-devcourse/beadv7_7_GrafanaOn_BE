package shop.deal.member.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import shop.deal.common.exception.BusinessException;
import shop.deal.member.application.MemberService;
import shop.deal.member.application.dto.MemberInfo;
import shop.deal.member.domain.exception.MemberErrorCode;
import shop.deal.member.presentation.dto.SignUpRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberSignUpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private SignUpRequest validRequest() {
        return new SignUpRequest(
                "테스트",
                "test@example.com",
                "abc12345!",
                "서울시 강남구",
                "010-1234-5678"
        );
    }

    @Test
    @DisplayName("회원가입에 성공하면 상태코드 200과 회원 정보를 반환한다")
    void signUp_success() throws Exception {
        given(memberService.signUp(any()))
                .willReturn(new MemberInfo(1L, "테스트", "test@example.com"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("테스트"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 상태코드 400과 DUPLICATE_EMAIL 에러코드를 반환한다")
    void signUp_duplicateEmail() throws Exception {
        given(memberService.signUp(any()))
                .willThrow(new BusinessException(MemberErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/members/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.DUPLICATE_EMAIL.getValue()))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_EMAIL.getMessage()));
    }

    @Test
    @DisplayName("입력데이터 형식이 맞지 않으면 400과 INVALID_INPUT 에러코드를 반환한다")
    void signUp_validPassword() throws Exception{
        SignUpRequest request = new SignUpRequest(
                "",
                "testexample.com",
                "abc12345",
                "",
                "010-1234-56789"
        );

        mockMvc.perform(post("/api/members/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.INVALID_INPUT.getValue()))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.INVALID_INPUT.getMessage()));
    }

}
