package shop.dear.identity.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import shop.dear.identity.member.application.dto.UpdateSellerAccountCommand;

public record UpdateSellerAccountRequest(
    @NotBlank String bank,
    @NotBlank
    @Pattern(
        regexp = "^\\d+$",
        message = "계좌번호는 숫자만 입력 가능합니다."
    )
    String account
){

    public UpdateSellerAccountCommand toCommand() {
        return new UpdateSellerAccountCommand(bank, account);
    }
}
