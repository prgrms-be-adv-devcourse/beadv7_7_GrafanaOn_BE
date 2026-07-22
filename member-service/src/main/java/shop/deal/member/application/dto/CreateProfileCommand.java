package shop.deal.member.application.dto;

public record CreateProfileCommand (
    String name,
    String defaultShippingAddress,
    String phoneNumber
){
}
