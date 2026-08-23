package shop.dear.identity.auth.authentication.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import shop.dear.identity.auth.authentication.application.AuthService;
import shop.dear.identity.auth.authentication.application.dto.SocialLoginCommand;
import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authentication.domain.AuthProvider;
import shop.dear.identity.auth.authentication.presentation.RefreshTokenCookieProvider;

import java.io.IOException;

/**
 * Google 인증이 끝난 직후 우리의 토큰을 발급하고 프론트로 돌려보닌다.
 *
 * Access Token은 쿼리 파라미터로 보내지 않고 비밀번호 로그인과 똑같이 Refresh Token을
 * httpOnly 쿠키로 심고, 프론트가 특정 API 주소로 Access Token을 받아간다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @Value("${auth.oauth2.success-redirect-uri}")
    private String successRedirectUri;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        TokenResult result = authService.loginWithSocial(new SocialLoginCommand(
                AuthProvider.GOOGLE,
                oAuth2User.getAttribute("sub"),
                oAuth2User.getAttribute("email"),
                Boolean.TRUE.equals(oAuth2User.getAttribute("email_verified")),
                oAuth2User.getAttribute("name")
        ));

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookieProvider.create(result.refreshToken(), result.refreshTokenExpiresInSeconds())
                        .toString()
        );

        response.sendRedirect(successRedirectUri);
    }
}