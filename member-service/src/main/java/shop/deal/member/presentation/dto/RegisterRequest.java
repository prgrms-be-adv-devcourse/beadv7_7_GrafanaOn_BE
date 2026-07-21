package shop.deal.member.presentation.dto;

import shop.deal.member.application.dto.RegisterCommand;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String defaultShippingAddress,
        String phoneNumber
) {
    public RegisterCommand toCommand() {
        return new RegisterCommand(name, email, password, defaultShippingAddress, phoneNumber);
    }
}
