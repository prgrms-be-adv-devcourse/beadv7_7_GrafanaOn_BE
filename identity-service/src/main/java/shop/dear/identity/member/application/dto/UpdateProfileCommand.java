package shop.dear.identity.member.application.dto;

public record UpdateProfileCommand (
    String defaultShippingAddress,
    String phoneNumber,
    String nickname
){
}
