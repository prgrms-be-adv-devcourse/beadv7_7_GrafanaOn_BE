package shop.dear.identity.member.application.dto;

public record CreateProfileCommand (
    String name,
    String defaultShippingAddress,
    String phoneNumber
){
}
