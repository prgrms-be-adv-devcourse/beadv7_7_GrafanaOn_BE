package shop.dear.identity.auth.authentication.application.dto;

import shop.dear.identity.auth.authentication.domain.AuthProvider;

public record SocialLoginCommand(
        AuthProvider provider,
        String providerId,
        String email,
        boolean emailVerified,
        String name
) {
}
