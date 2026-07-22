package shop.dear.identity.auth.authentication.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import shop.dear.identity.auth.authentication.application.dto.SignUpCommand;

// 회원가입 요청 DTO
public record SignUpRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Size(max = 30)
        String name,

        @NotBlank
        @Size(max = 255)
        String defaultShippingAddress,

        @NotBlank
        @Pattern(
                regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
                message = "휴대폰 번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                email,
                password,
                name,
                defaultShippingAddress,
                phoneNumber
        );
    }
}
