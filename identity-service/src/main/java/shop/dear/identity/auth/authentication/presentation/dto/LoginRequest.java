package shop.dear.identity.auth.authentication.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import shop.dear.identity.auth.authentication.application.dto.LoginCommand;

// 로그인 요청
public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(
                email,
                password
        );
    }
}
