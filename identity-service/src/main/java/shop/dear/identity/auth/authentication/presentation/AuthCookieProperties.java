package shop.dear.identity.auth.authentication.presentation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.cookie")
public record AuthCookieProperties(
        @NotBlank
        String name, // Refresh Token 쿠키 이름
        boolean secure, // HTTPS에서만 쿠키 전송할 지 여부

        @NotBlank
        String sameSite, // 다른 사이트에서 쿠키를 전송할 수 있는가? (Strict, Lax, None)

        @NotBlank
        String path // 쿠키가 전송될 Auth API 경로
) {
}
