package shop.dear.identity.auth.authentication.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

// JWT 설정값 바인딩
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotBlank String issuer, // JWT 발급자
        @NotBlank String secret, // 서명용 BASE64 비밀 키
        @NotNull Duration accessTokenExpiration,
        @NotNull Duration refreshTokenExpiration
        ) {
}
