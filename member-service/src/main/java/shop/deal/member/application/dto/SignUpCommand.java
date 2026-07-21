package shop.deal.member.application.dto;

public record SignUpCommand (
        String name,
        String email,
        String password,
        String defaultShippingAddress,
        String phoneNumber
){
}
