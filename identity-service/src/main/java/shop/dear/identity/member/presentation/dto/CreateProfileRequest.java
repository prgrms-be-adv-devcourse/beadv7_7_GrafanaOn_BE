package shop.dear.identity.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import shop.dear.identity.member.application.dto.CreateProfileCommand;

public record CreateProfileRequest(
        @NotBlank String name,
        @NotBlank String defaultShippingAddress,
        @NotBlank
        @Pattern(
            regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
            message = "휴대폰 번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {

    public CreateProfileCommand toCommand() {
        return new CreateProfileCommand(name, defaultShippingAddress, phoneNumber);
    }
}
