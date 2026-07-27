package shop.dear.identity.auth.authentication.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {
    private final AuthCookieProperties properties;

    // 로그인, 재발급 성공 시 Refresh Token 쿠키 생성
    public ResponseCookie create(String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(properties.name(), refreshToken)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    // 로그아웃 시 동일한 이름과 경로의 쿠키 만료
    public ResponseCookie delete() {
        return ResponseCookie.from(properties.name(), "")
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(Duration.ZERO)
                .build();
    }
}
