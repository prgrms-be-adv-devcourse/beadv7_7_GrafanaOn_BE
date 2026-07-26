package shop.dear.identity.auth.authentication.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.auth.authentication.application.AuthService;
import shop.dear.identity.auth.authentication.application.dto.LogoutCommand;
import shop.dear.identity.auth.authentication.application.dto.ReissueTokenCommand;
import shop.dear.identity.auth.authentication.application.dto.SignUpResult;
import shop.dear.identity.auth.authentication.application.dto.TokenResult;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;
import shop.dear.identity.auth.authentication.presentation.dto.LoginRequest;
import shop.dear.identity.auth.authentication.presentation.dto.SignUpRequest;
import shop.dear.identity.auth.authentication.presentation.dto.SignUpResponse;
import shop.dear.identity.auth.authentication.presentation.dto.TokenResponse;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    // 공개 API
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(
            @Valid @RequestBody SignUpRequest request
            ) {
        SignUpResult result = authService.signUp(request.toCommand());

        return ResponseEntity.ok(
                successWithData(SignUpResponse.from(result))
        );
    }

    // 공개 API
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResult result = authService.login(request.toCommand());

        ResponseCookie refreshTokenCookie =
                refreshTokenCookieProvider.create(
                        result.refreshToken(),
                        result.refreshTokenExpiresInSeconds()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(
                        successWithData(TokenResponse.from(result))
                );
    }

    // Refresh Cookie 있어야 접근할 수 있는 API
    // Refresh Token을 쿠키에서 읽는다.
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(
                    name = "${auth.cookie.name}",
                    required = false
            )
            String refreshToken
    ) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );
        }

        TokenResult result = authService.reissue(
                new ReissueTokenCommand(refreshToken)
        );

        ResponseCookie refreshTokenCookie =
                refreshTokenCookieProvider.create(
                        result.refreshToken(),
                        result.refreshTokenExpiresInSeconds()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(
                        successWithData(TokenResponse.from(result))
                );
    }

    // Access Token이 있어야 접근할 수 있는 API
    // 커스텀 어노테이션 @AuthUser로 memberId를 받아온다.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthUser Long memberId) {
        authService.logout(new LogoutCommand(memberId));

        ResponseCookie deletedCookie = refreshTokenCookieProvider.delete();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deletedCookie.toString()
                )
                .body(success());
    }
}
