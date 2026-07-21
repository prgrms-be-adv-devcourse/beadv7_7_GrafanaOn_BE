package shop.deal.member.application.dto;

public record RegisterCommand (
        String name,
        String email,
        String password,
        String defaultShippingAddress,
        String phoneNumber
){
}
