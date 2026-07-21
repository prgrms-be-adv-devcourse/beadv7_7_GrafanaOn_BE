package shop.deal.member.application.dto;

public record SignUpCommand (
    String name,
    String email,
    String rawPassword,
    String defaultShippingAddress,
    String phoneNumber
){
}
