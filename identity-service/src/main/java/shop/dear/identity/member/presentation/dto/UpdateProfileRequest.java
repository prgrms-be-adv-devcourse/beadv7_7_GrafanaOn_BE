package shop.dear.identity.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import shop.dear.identity.member.application.dto.UpdateProfileCommand;

public record UpdateProfileRequest (
    @NotBlank String defaultShippingAddress,
    @NotBlank
    @Pattern(
        regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
        message = "휴대폰 번호 형식이 올바르지 않습니다."
    )
    String phoneNumber,
    @NotBlank
    @Size (max = 30, message = "닉네임은 최대 30글자까지 가능합니다.")
    String nickname
){

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(defaultShippingAddress, phoneNumber, nickname);
    }
}
