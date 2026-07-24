package shop.dear.identity.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import shop.dear.identity.member.application.dto.UpdateSellerAccountCommand;

public record UpdateSellerAccountRequest(
    @NotBlank String bank,
    @NotBlank String account
){

    public UpdateSellerAccountCommand toCommand() {
        return new UpdateSellerAccountCommand(bank, account);
    }
}
