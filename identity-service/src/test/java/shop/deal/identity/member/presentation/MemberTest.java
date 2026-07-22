package shop.deal.identity.member.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.deal.identity.member.application.MemberService;
import shop.deal.identity.member.application.dto.MemberInfo;
import shop.deal.identity.member.domain.exception.MemberErrorCode;
import shop.deal.identity.member.presentation.dto.UpdateProfileRequest;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
public class MemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private UpdateProfileRequest validUpdateRequest() {

        return new UpdateProfileRequest(
            "서울시 강남구",
            "010-1234-5678",
            "다람쥐"
        );
    }

    @Test
    @DisplayName("memberId로 프로필 조회에 성공하면 상태코드 200과 회원 정보를 반환한다")
    void getProfile_success() throws Exception {

        given(memberService.getProfile(1L))
            .willReturn(new MemberInfo(
                    1L,
                    "테스트",
                    "서울시 강남구",
                    "010-1234-5678",
                    "user_000001"
                )
            );

        final ResultActions result = mockMvc
            .perform(get("/api/members/profile")
                .param("memberId", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("테스트"))
            .andExpect(jsonPath("$.data.defaultShippingAddress").value("서울시 강남구"))
            .andExpect(jsonPath("$.data.phoneNumber").value("010-1234-5678"))
            .andExpect(jsonPath("$.data.nickname").value("user_000001"));
    }

    @Test
    @DisplayName("프로필 조회에 성공하면 상태코드 200과 회원 정보를 반환한다")
    void getMyProfile_success() throws Exception {

        given(memberService.getProfile(1L))
            .willReturn(new MemberInfo(
                    1L,
                    "테스트",
                    "서울시 강남구",
                    "010-1234-5678",
                    "user_000001"
                )
            );

        final ResultActions result = mockMvc
            .perform(get("/api/members/profile/me")
                .header("X-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("테스트"))
            .andExpect(jsonPath("$.data.defaultShippingAddress").value("서울시 강남구"))
            .andExpect(jsonPath("$.data.phoneNumber").value("010-1234-5678"))
            .andExpect(jsonPath("$.data.nickname").value("user_000001"));
    }

    @Test
    @DisplayName("동시 요청으로 닉네임이 충돌하면 상태코드 409와 DUPLICATE_NICKNAME 에러코드를 반환한다")
    void updateProfile_concurrentNicknameConflict() throws Exception {

        given(memberService.updateProfile(any(), any()))
            .willThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        final ResultActions result = mockMvc
            .perform(patch("/api/members/profile/me")
                .header("X-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validUpdateRequest())));

        result
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.DUPLICATE_NICKNAME.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }
}
