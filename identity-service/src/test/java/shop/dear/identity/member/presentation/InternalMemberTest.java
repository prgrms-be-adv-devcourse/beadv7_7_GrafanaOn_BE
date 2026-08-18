package shop.dear.identity.member.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.request.CreateProfileRequest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.identity.member.application.dto.MemberInfo;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalMemberController.class)
class InternalMemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private CreateProfileRequest validRequest() {

        return new CreateProfileRequest(
            "테스트",
            "서울시 강남구",
            "010-1234-5678"
        );
    }

    @Test
    @DisplayName("프로필 생성에 성공하면 상태코드 200과 memberId를 반환한다")
    void createProfile_success() throws Exception {

        given(memberService.createProfile(any()))
            .willReturn(new MemberInfo(
                    1L,
                    "테스트",
                    "서울시 강남구",
                    "010-1234-5678",
                    "user_000001"
                )
            );

        final ResultActions result = mockMvc
            .perform(post("/internal/members")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validRequest())));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.memberId").value(1L));
    }

    @Test
    @DisplayName("입력데이터 형식이 맞지 않으면 상태코드 400과 INVALID_INPUT 에러코드를 반환한다")
    void createProfile_invalidInput() throws Exception{

        CreateProfileRequest request = new CreateProfileRequest(
            "",
            "",
            "010-1234-56789"
        );

        final ResultActions result = mockMvc
            .perform(post("/internal/members")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.INVALID_INPUT.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.INVALID_INPUT.getMessage()));
    }

    @Test
    @DisplayName("유효한 회원을 조회하면 exists true를 반환한다")
    void isActiveMember_true() throws Exception {

        given(memberService.isActiveMember(1L)).willReturn(true);

        final ResultActions result = mockMvc
            .perform(get("/internal/members")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    @DisplayName("탈퇴한 회원을 조회하면 exists false를 반환한다")
    void isActiveMember_false() throws Exception {

        given(memberService.isActiveMember(1L)).willReturn(false);

        final ResultActions result = mockMvc
            .perform(get("/internal/members")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.exists").value(false));
    }

    @Test
    @DisplayName("판매자인 회원을 조회하면 true를 반환한다")
    void isSeller_true() throws Exception {

        given(memberService.isSeller(1L)).willReturn(true);

        final ResultActions result = mockMvc
            .perform(get("/internal/members/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.isSeller").value(true));
    }

    @Test
    @DisplayName("판매자가 아닌 회원을 조회하면 false를 반환한다")
    void isSeller_false() throws Exception {

        given(memberService.isSeller(1L)).willReturn(false);

        final ResultActions result = mockMvc
            .perform(get("/internal/members/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.isSeller").value(false));
    }
}
