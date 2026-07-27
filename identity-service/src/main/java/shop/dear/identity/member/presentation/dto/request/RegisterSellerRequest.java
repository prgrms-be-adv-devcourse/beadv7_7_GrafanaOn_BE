package shop.dear.identity.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import shop.dear.identity.member.application.dto.RegisterSellerCommand;

public record RegisterSellerRequest(
    @NotBlank String bank,
    @NotBlank String account
){

    public RegisterSellerCommand toCommand() {
        return new RegisterSellerCommand(bank, account);
    }
}
