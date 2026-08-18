package shop.dear.identity.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import shop.dear.identity.member.application.dto.RegisterSellerCommand;

public record RegisterSellerRequest(
    @NotBlank String bank,
    @NotBlank
    @Pattern(
        regexp = "^\\d+$",
        message = "계좌번호는 숫자만 입력 가능합니다."
    )
    String account
){

    public RegisterSellerCommand toCommand() {
        return new RegisterSellerCommand(bank, account);
    }
}
