package shop.deal.member.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import shop.deal.member.application.dto.SignUpCommand;

public record SignUpRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8~20자로 입력해주세요."
        )
        String password,
        @NotBlank String defaultShippingAddress,
        @NotBlank
        @Pattern(
                regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
                message = "휴대폰 번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {

    public SignUpCommand toCommand() {
        return new SignUpCommand(name, email, password, defaultShippingAddress, phoneNumber);
    }
}
