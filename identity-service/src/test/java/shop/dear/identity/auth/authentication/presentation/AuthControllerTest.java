package shop.dear.identity.auth.authentication.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.identity.auth.authentication.application.AuthService;
import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authentication.application.dto.WithdrawCommand;
import shop.dear.identity.auth.authentication.presentation.dto.LoginRequest;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(RefreshTokenCookieProvider.class)
@EnableConfigurationProperties(AuthCookieProperties.class)
@TestPropertySource(properties = {
        "auth.cookie.name=refreshToken",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Strict",
        "auth.cookie.path=/api/auth"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공 시 Access Token은 응답에, Refresh Token은 HttpOnly Cookie에 담김.")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest(
                "buyer@example.com",
                "password123"
        );

        TokenResult tokenResult = new TokenResult(
                "access-token",
                "refresh-token",
                3600L,
                1209600L
        );

        given(authService.login(any()))
                .willReturn(tokenResult);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.accessToken")
                                .value("access-token")
                )
                .andExpect(
                        jsonPath("$.data.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.data.refreshToken")
                                .doesNotExist()
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "refreshToken=refresh-token"
                                )
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("HttpOnly")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("SameSite=Strict")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("Path=/api/auth")
                        )
                );
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 인증 계정을 탈퇴 처리하고 Refresh Token 쿠키를 삭제한다")
    void withdrawSuccess() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/api/auth/withdraw")
                                .header(
                                        "X-Authenticated-Member-Id",
                                        "1"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("refreshToken=")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("Max-Age=0")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("HttpOnly")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("Path=/api/auth")
                        )
                );

        verify(authService).withdraw(
                new WithdrawCommand(1L)
        );
    }
}