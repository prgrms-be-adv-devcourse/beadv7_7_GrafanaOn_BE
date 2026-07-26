package shop.dear.identity.member.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.application.dto.MemberInfo;
import shop.dear.identity.member.application.dto.SellerInfo;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.presentation.dto.request.RegisterSellerRequest;
import shop.dear.identity.member.presentation.dto.request.SellerCheckRequest;
import shop.dear.identity.member.presentation.dto.request.UpdateProfileRequest;
import shop.dear.identity.member.presentation.dto.request.UpdateSellerAccountRequest;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .header("X-Authenticated-Member-Id", "1"));

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
                .header("X-Authenticated-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validUpdateRequest())));

        result
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.DUPLICATE_NICKNAME.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }

    @Test
    @DisplayName("판매자 등록에 성공하면 상태코드 200을 반환한다")
    void registerSeller_success() throws Exception {

        RegisterSellerRequest request = new RegisterSellerRequest("국민은행", "123-456-789");

        final ResultActions result = mockMvc
            .perform(post("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"));
    }

    @Test
    @DisplayName("이미 판매자로 등록된 회원이 판매자 등록을 요청하면 상태코드 400과 ALREADY_SELLER 에러코드를 반환한다")
    void registerSeller_alreadySeller() throws Exception {

        RegisterSellerRequest request = new RegisterSellerRequest("국민은행", "123-456-789");

        willThrow(new BusinessException(MemberErrorCode.ALREADY_SELLER))
            .given(memberService).registerSeller(eq(1L), any());

        final ResultActions result = mockMvc
            .perform(post("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.ALREADY_SELLER.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.ALREADY_SELLER.getMessage()));
    }

    @Test
    @DisplayName("판매자 계좌 수정에 성공하면 상태코드 200을 반환한다")
    void updateSellerAccount_success() throws Exception {

        UpdateSellerAccountRequest request = new UpdateSellerAccountRequest("신한은행", "987-654-321");

        final ResultActions result = mockMvc
            .perform(patch("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"));
    }

    @Test
    @DisplayName("판매자가 아닌 회원이 계좌 수정을 요청하면 상태코드 400과 NOT_SELLER 에러코드를 반환한다")
    void updateSellerAccount_notSeller() throws Exception {

        UpdateSellerAccountRequest request = new UpdateSellerAccountRequest("신한은행", "987-654-321");

        willThrow(new BusinessException(MemberErrorCode.NOT_SELLER))
            .given(memberService).updateSellerAccount(eq(1L), any());

        final ResultActions result = mockMvc
            .perform(patch("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.NOT_SELLER.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.NOT_SELLER.getMessage()));
    }

    @Test
    @DisplayName("판매자 등록 해지에 성공하면 상태코드 200을 반환한다")
    void unRegister_success() throws Exception {

        final ResultActions result = mockMvc
            .perform(delete("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"));
    }

    @Test
    @DisplayName("등록된 판매상품이 있으면 판매자 등록 해지 요청에 상태코드 400과 WITHDRAWAL_FAILED 에러코드를 반환한다")
    void unRegister_hasProduct() throws Exception {

        willThrow(new BusinessException(MemberErrorCode.WITHDRAWAL_FAILED))
            .given(memberService).unRegister(1L);

        final ResultActions result = mockMvc
            .perform(delete("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.WITHDRAWAL_FAILED.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.WITHDRAWAL_FAILED.getMessage()));
    }

    @Test
    @DisplayName("판매자 계좌 조회에 성공하면 상태코드 200과 마스킹된 계좌 정보를 반환한다")
    void getMyAccount_success() throws Exception {

        given(memberService.getMyAccount(1L))
            .willReturn(new SellerInfo("국민은행", "123*****789"));

        final ResultActions result = mockMvc
            .perform(get("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.bank").value("국민은행"))
            .andExpect(jsonPath("$.data.account").value("123*****789"));
    }

    @Test
    @DisplayName("판매자가 아닌 회원이 계좌 조회를 요청하면 상태코드 400과 NOT_SELLER 에러코드를 반환한다")
    void getMyAccount_notSeller() throws Exception {

        given(memberService.getMyAccount(1L))
            .willThrow(new BusinessException(MemberErrorCode.NOT_SELLER));

        final ResultActions result = mockMvc
            .perform(get("/api/members/me/seller")
                .header("X-Authenticated-Member-Id", "1"));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(MemberErrorCode.NOT_SELLER.getValue()))
            .andExpect(jsonPath("$.message").value(MemberErrorCode.NOT_SELLER.getMessage()));
    }

    @Test
    @DisplayName("판매자인 회원을 조회하면 상태코드 200과 isSeller=true를 반환한다")
    void isSeller_true() throws Exception {

        given(memberService.isSeller(1L)).willReturn(true);

        SellerCheckRequest request = new SellerCheckRequest(1L);

        final ResultActions result = mockMvc
            .perform(get("/api/members/seller")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isSeller").value(true));
    }

    @Test
    @DisplayName("판매자가 아닌 회원을 조회하면 상태코드 200과 isSeller=false를 반환한다")
    void isSeller_false() throws Exception {

        given(memberService.isSeller(1L)).willReturn(false);

        SellerCheckRequest request = new SellerCheckRequest(1L);

        final ResultActions result = mockMvc
            .perform(get("/api/members/seller")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isSeller").value(false));
    }
}
