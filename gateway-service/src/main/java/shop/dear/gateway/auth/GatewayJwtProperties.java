package shop.dear.gateway.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record GatewayJwtProperties (
        // Identity가 JWT를 발급한 주체인지 확인
        @NotBlank String issuer,
        // JWT 서명 검증
        @NotBlank String secret
) {
}
